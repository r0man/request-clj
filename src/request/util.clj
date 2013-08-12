(ns request.util
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]))

(def route-keys
  [:method :route-name :path :path-params])

(defn format-uri
  "Format the `route` url by expanding :params in `opts`."
  [route & [opts]]
  (reduce
   (fn [uri param]
     (let [params (:params opts)]
       (if-let [value (-> params param)]
         (replace uri (str param) (str value))
         (throw (ex-info (format "Can't expand query param %s." param) params)))))
   (:path route) (:path-params route)))

(defn make-request
  "Find the route `name` in `routes` and return the Ring request."
  [routes name & [opts]]
  (when-let [route (get routes (keyword name))]
    (-> (merge route opts)
        (assoc :uri (format-uri route opts)))))

(defn path-for-routes
  "Returns a fn that generates the path of `routes`."
  [routes]
  (fn [name & opts]
    (let [request (apply make-request routes name opts)]
      (:uri request))))

(defn url-for-routes
  "Returns a fn that generates the url of `routes`."
  [routes]
  (fn [route-name & opts]
    (let [{:keys [scheme server-name server-port uri]}
          (apply make-request routes route-name opts)]
      (str
       (if scheme (name scheme) "http")
       "://"
       (if server-name server-name "localhost")
       (if (and server-port
                (not (and (= scheme :http)
                          (= server-port 80)))
                (not (and (= scheme :https)
                          (= server-port 443))))
         (str ":" server-port))
       uri))))

(defn select-routes
  "Select all neccesary routing keys from the `routes`."
  [routes]
  (->> (map #(select-keys %1 route-keys) routes)
       (sort-by :route-name)))
