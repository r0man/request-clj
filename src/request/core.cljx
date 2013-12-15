(ns request.core
  (:refer-clojure :exclude [replace])
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :refer [blank? replace]]
            [no.en.core :refer [format-query-params format-url]]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.core.async :refer [<! chan close! go put!]]
            #+clj [slingshot.slingshot :refer [try+]]
            #+clj [clj-http.client :as clj-http]
            #+cljs [cljs-http.client :as cljs-http]
            #+cljs [cljs.core.async :refer [<! chan close! put!]]))

(def route-keys
  [:method :route-name :path :path-params])

(defn expand-path
  "Format the `route` url by expanding :path-params in `opts`."
  [route & [opts]]
  (reduce
   (fn [uri param]
     (let [params (or (:path-params opts) (:edn-body opts) opts)]
       (if-let [value (-> params param)]
         (replace uri (str param) (str value))
         (throw (ex-info (str "Can't expand query param: " param)
                         {:path (:path route)
                          :params params})))))
   (:path route) (:path-params route)))

(defn make-request
  "Find the route `name` in `routes` and return the Ring request."
  [routes name & [opts]]
  (if-let [route (get routes (keyword name))]
    (assoc (merge {:scheme :http :server-name "localhost"} route opts)
      :uri (expand-path route opts))
    (throw (ex-info (str "Can't find route: " name) routes))))

(defn path-for-routes
  "Returns a fn that generates the path of `routes`."
  [routes]
  (fn [name & [opts]]
    (let [request (make-request routes name opts)
          query (format-query-params (:query-params opts))]
      (str (:uri request) (if-not (blank? query) (str "?" query))))))

(defn url-for-routes
  "Returns a fn that generates the url of `routes`."
  [routes]
  (fn [route-name & [opts]]
    (format-url (make-request routes route-name opts))))

(defn select-routes
  "Select all neccesary routing keys from the `routes`."
  [routes]
  (->> (map #(select-keys %1 route-keys) routes)
       (sort-by :route-name)))

(defn unpack-response [response]
  (let [body (:body response)]
    (if (or (map? body)
            (sequential? body))
      (with-meta body (dissoc response :body))
      body)))

(defn wrap-edn-body [client]
  (fn [request]
    (if (:edn-body request)
      (-> (dissoc request :edn-body)
          (assoc :body (pr-str (:edn-body request)))
          (assoc-in [:headers "Content-Type"] "application/edn")
          (client))
      (client request))))

#+clj
(defn wrap-pagination [client & [per-page]]
  (letfn [(paginate [request & [page per-page]]
            (update-in
             (-> request
                 (assoc-in [:query-params :page] page)
                 (assoc-in [:query-params :per-page] per-page)
                 (client))
             [:body]
             #(if (sequential? %1)
                (lazy-seq
                 (if-not (empty? %1)
                   (concat %1 (:body (paginate request (inc page) per-page)))
                   %1))
                %1)))]
    (fn [request]
      (if (or (= :get (:method request))
              (-> request :query-params :page))
        (client request)
        (clj-http/with-connection-pool {}
          (paginate request 1 (or per-page 100)))))))

;; PLATFORM

(def client
  #+clj
  (->  #'clj-http/request
       (wrap-edn-body)
       (wrap-pagination))
  #+cljs
  (->  cljs-http/request
       (wrap-edn-body)))

(defn http
  "Make a HTTP request and return the response."
  [routes name & [opts]]
  #+clj
  (-> (make-request routes name opts)
      (assoc :throw-exceptions false)
      (client))
  #+cljs
  (throw js/Error "Not implemented on JavaScript runtime."))

(defn http!
  "Make a HTTP request and return the response."
  [routes name & [opts]]
  #+clj
  (-> (make-request routes name opts)
      (assoc :throw-exceptions true)
      (client))
  #+cljs
  (throw js/Error "Not implemented on JavaScript runtime."))

(defn http<!
  "Make a HTTP request and return a core.async channel."
  [routes name & [opts]]
  #+clj
  (let [channel (chan)]
    (go (try+ (->> (make-request routes name opts)
                   (client)
                   (>! channel))
              (catch map? response
                (>! channel response))
              (catch Object _
                (>! channel (:throwable &throw-context)))
              (finally (close! channel))))
    channel)
  #+cljs
  (client (make-request routes name opts)))

(defn body
  "Make a HTTP request and return the body of response."
  [routes name & [opts]]
  #+clj (unpack-response (http routes name opts))
  #+cljs (throw js/Error "Not implemented on JavaScript runtime."))

(defn body<!
  "Make a HTTP request and return the body in a core.async channel."
  [routes name & [opts]]
  (let [channel (chan)]
    (go (let [response (<! (http<! routes name opts))]
          (put! channel (unpack-response response))
          (close! channel)))
    channel))

(defn- update-path-re [route]
  (update-in route [:path-re] #(if %1 (re-pattern %1))))

(defn- zip-routes [routes & [opts]]
  (zipmap (map :route-name routes)
          (map #(merge opts %1) routes)))

#+clj
(defn fetch-routes
  "Fetch the route specification from `url`."
  [url]
  (->> (:body (client {:method :get :url url :as :auto}))
       (map update-path-re)
       (zip-routes)))

#+clj
(defn read-routes
  "Read the route specification from `filename`."
  [filename]
  (->> (edn/read-string (slurp filename))
       (map update-path-re)
       (zip-routes)))

(defmacro defroutes [name routes & [opts]]
  `(do (def ~name
         (let [routes# ~routes
               opts# ~opts]
           (zipmap (map :route-name routes#)
                   (map (partial merge opts#) routes#))))
       (def ~'path-for (request.core/path-for-routes ~name))
       (def ~'url-for (request.core/url-for-routes ~name))
       (defn ~'body [~'route & [~'opts]]
         (request.core/body ~name ~'route ~'opts))
       (defn ~'body<! [~'route & [~'opts]]
         (request.core/body<! ~name ~'route ~'opts))
       (defn ~'http [~'route & [~'opts]]
         (request.core/http ~name ~'route ~'opts))
       (defn ~'http! [~'route & [~'opts]]
         (request.core/http! ~name ~'route ~'opts))
       (defn ~'http<! [~'route & [~'opts]]
         (request.core/http<! ~name ~'route ~'opts))
       (defn ~'request [~'route & [~'opts]]
         (request.core/make-request ~name ~'route ~'opts))))

(comment
  ((url-for-routes (read-routes "test-resources/routes.edn")) :spots)
  (clojure.pprint/pprint (read-routes "test-resources/routes.edn"))
  (first (:spots (read-routes "test-resources/routes.edn")))
  (first (fetch-routes "http://api.burningswell.dev/routes")))
