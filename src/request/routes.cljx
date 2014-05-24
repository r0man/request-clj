(ns request.routes
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? replace]]
            [no.en.core :refer [format-query-params format-url]]
            [request.core :as core]
            #+clj [clojure.pprint :refer [pprint]]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.core.async :refer [<! chan close! map< go put!]]
            #+clj [slingshot.slingshot :refer [try+]]
            #+clj [clj-http.client :as clj-http]
            #+cljs [cljs-http.client :as cljs-http]
            #+cljs [cljs.core.async :refer [<! chan close! map< put!]])
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]]))

(def route-keys
  [:method :route-name :path :path-params :path-re])

(defn assoc-route [routes route-name path-re & [opts]]
  (let [route (merge {:method :get} opts)
        route (assoc route :route-name route-name :path-re path-re)]
    (assoc routes route-name route)))

(defn- check-request [request]
  (if-not (or (:uri request) (:url request))
    (throw (ex-info "HTTP request is missing :uri or :url." {:request request}))
    request))

(defn find-route
  "Lookup the route `name` by keyword in `rou"
  [routes name]
  (get routes (keyword name)))

(defn expand-path
  "Format the `route` url by expanding :path-params in `opts`."
  [route & [opts]]
  (reduce
   (fn [uri param]
     (let [params (or (:path-params opts) (:edn-body opts) opts)]
       (if-let [value (-> params param)]
         (replace uri (str param) (str value))
         uri)))
   (:path route) (:path-params route)))

(defn resolve-route
  "Find the route `name` in `routes` and return the Ring request."
  ([request]
     request)
  ([routes request]
     (if (map? request)
       (resolve-route routes nil request)
       (resolve-route routes request nil)))
  ([routes name request]
     (if-let [route (find-route routes name)]
       (assoc (merge {:scheme :http :server-name "localhost"} route request)
         :uri (expand-path route request))
       request)))

(defn- match-path [path route]
  (if-let [matches (re-matches (:path-re route) path)]
    (assoc route
      :uri path
      :path-params (zipmap (:path-params route) (rest matches)))))

(defn path-matches
  [routes path & [method]]
  (let [method (or method :get)]
    (->> (vals routes)
         (filter #(= method (:method %1)))
         (map (partial match-path path))
         (remove nil?))))

(defn path-for-routes
  "Returns a fn that generates the path of `routes`."
  [routes]
  (fn [route-name & [opts]]
    (if (find-route routes route-name)
      (let [request (resolve-route routes route-name opts)
            query (format-query-params (:query-params opts))]
        (str (:uri request) (if-not (blank? query) (str "?" query)))))))

(defn url-for-routes
  "Returns a fn that generates the url of `routes`."
  [routes]
  (fn [route-name & [opts]]
    (if (find-route routes route-name)
      (format-url (resolve-route routes route-name opts)))))

(defn strip-path-re [route]
  (update-in route [:path-re] #(if %1 (replace %1 #"\\Q|\\E" ""))))

(defn http
  "Make a HTTP request and return the response."
  [routes name & [opts]]
  (core/http (resolve-route routes name opts)))

(defn http!
  "Make a HTTP request and return the response."
  [routes name & [opts]]
  (core/http! (resolve-route routes name opts)))

(defn http<
  "Make a HTTP request and return a core.async channel."
  [routes name & [opts]]
  (core/http< (resolve-route routes name opts)))

(defn body
  "Make a HTTP request and return the body of response."
  [routes name & [opts]]
  (core/with-meta-resp (core/http (resolve-route routes name opts)))
  #+cljs (throw js/Error "Not implemented on JavaScript runtime."))

(defn body<
  "Make a HTTP request and return the body in a core.async channel."
  [routes name & [opts]]
  (map< core/with-meta-resp (core/http< (resolve-route routes name opts))))

(defn serialize-route [route]
  (update-in route [:path-re] #(if %1 (str %1))))

(defn deserialize-route [route]
  (update-in route [:path-re] #(if %1 (re-pattern %1))))

(defn- zip-routes [routes & [opts]]
  (zipmap (map :route-name routes)
          (map #(merge opts %1) routes)))

#+clj
(defn fetch-routes
  "Fetch the route specification from `url`."
  [url]
  (->> (:body (core/client {:method :get :url url :as :auto}))
       (map deserialize-route)
       (zip-routes)))

#+clj
(defn read-routes
  "Read the routes in EDN format from `filename`."
  [filename]
  (->> (edn/read-string (slurp filename))
       (map deserialize-route)
       (zip-routes)))

#+clj
(defn spit-routes
  "Spit the `routes` in EDN format to `filename`."
  [filename routes]
  (spit filename
        (with-out-str
          (->> (vals routes)
               (map serialize-route)
               (sort-by :route-name)
               (pprint)))))

(defmacro defroutes [name routes & [opts]]
  `(do (def ~name
         (let [routes# ~routes
               opts# ~opts]
           (zipmap (map :route-name routes#)
                   (map (partial merge opts#) routes#))))
       (def ~'path-for (request.routes/path-for-routes ~name))
       (def ~'url-for (request.routes/url-for-routes ~name))
       (defn ~'body [~'route & [~'opts]]
         (request.routes/body ~name ~'route ~'opts))
       (defn ~'body< [~'route & [~'opts]]
         (request.routes/body< ~name ~'route ~'opts))
       (defn ~'http [~'route & [~'opts]]
         (request.routes/http ~name ~'route ~'opts))
       (defn ~'http! [~'route & [~'opts]]
         (request.routes/http! ~name ~'route ~'opts))
       (defn ~'http< [~'route & [~'opts]]
         (request.routes/http< ~name ~'route ~'opts))
       (defn ~'make-request [~'route & [~'opts]]
         (request.routes/resolve-route ~name ~'route ~'opts))))

(comment
  (require '[clojure.pprint :refer [pprint]])
  (def r (read-routes "test-resources/routes.edn"))
  (clojure.pprint/pprint (read-routes "test-resources/routes.edn"))
  (first (:spots (read-routes "test-resources/routes.edn")))
  (first (fetch-routes "http://api.burningswell.dev/routes")))
