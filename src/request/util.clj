(ns request.util
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? replace]]
            [no.en.core :refer [format-query-params format-url]]))

(def route-keys
  [:method :route-name :path :path-params])

(defn expand-path
  "Format the `route` url by expanding :path-params in `opts`."
  [route & [opts]]
  (reduce
   (fn [uri param]
     (let [params (or (:path-params opts) opts)]
       (if-let [value (-> params param)]
         (replace uri (str param) (str value))
         (throw (ex-info (format "Can't expand query param %s." param) params)))))
   (:path route) (:path-params route)))

(defn make-request
  "Find the route `name` in `routes` and return the Ring request."
  [routes name & [opts]]
  (if-let [route (get routes (keyword name))]
    (-> (merge route opts)
        (assoc :uri (expand-path route opts)))
    (throw (ex-info (format "Can't find route %s." name) routes))))

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
