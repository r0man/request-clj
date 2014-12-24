(ns request.core
  (:refer-clojure :exclude [get replace])
  (:require [clojure.string :refer [blank? replace]]
            [no.en.core :refer [format-query-params format-url parse-url]]
            [routes.core :as routes]
            #+clj [clojure.pprint :refer [pprint]]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.core.async :refer [<! >! chan close! map< go put!]]
            #+clj [clj-http.client :as http]
            #+cljs [cljs-http.client :as http])
  #+cljs (:import goog.Uri))

(defrecord Client [backend pool router])

(defprotocol IRequest
  (to-request [x] "Convert `x` into an HTTP request"))

(defn- check-request [request]
  (if-not (or (:uri request) (:url request))
    (throw (ex-info "HTTP request is missing :uri or :url." {:request request}))
    request))

(defn new-client
  "Return a new HTTP client using `config`."
  [& [config]]
  (-> (merge
       {:as :auto
        :backend #+clj #'http/request #+cljs http/request
        :coerce :auto
        :throw-exceptions false}
       config)
      (map->Client)))

(defn send-request
  "Send the HTTP `request` via `client`."
  [client request]
  (let [request (to-request request)]
    ((:backend client) (merge (into {} client) request))))

(defn request
  "Return the HTTP request for `route`."
  [client route & [opts]]
  (if (keyword? route)
    (let [ex-data {:client client :route route :opts opts}]
      (if-let [router (:router client)]
        (or (routes/request-for router client route opts)
            (throw (ex-info "Can't resolve route." ex-data)))
        (throw (ex-info "No routes defined for client." ex-data))))
    (merge (to-request route) client opts)))

(defn send-method
  "Send the HTTP `request` as `verb` via `client`."
  [client verb req & [opts]]
  (->> (assoc (request client req opts)
         :method verb
         :request-method verb)
       (send-request client)))

(defmacro defroutes
  "Define routes and a client."
  [name routes & opts]
  `(do (routes.core/defroutes ~name ~routes ~@opts)
       (defn ~'new-client [& [~'config]]
         (request.core/new-client
          (merge {:router ~name }
                 (hash-map ~@opts)
                 ~'config)))))

;; HTTP methods

(defn connect
  "Send the HTTP CONNECT `request` via `client`."
  [client request & [opts]]
  (send-method client :connect request opts))

(defn delete
  "Send the HTTP DELETE `request` via `client`."
  [client request & [opts]]
  (send-method client :delete request opts))

(defn get
  "Send the HTTP GET `request` via `client`."
  [client request & [opts]]
  (send-method client :get request opts))

(defn head
  "Send the HTTP HEAD `request` via `client`."
  [client request & [opts]]
  (send-method client :head request opts))

(defn options
  "Send the HTTP OPTIONS `request` via `client`."
  [client request & [opts]]
  (send-method client :options request opts))

(defn patch
  "Send the HTTP PATCH `request` via `client`."
  [client request & [opts]]
  (send-method client :patch request opts))

(defn post
  "Send the HTTP POST `request` via `client`."
  [client request & [opts]]
  (send-method client :post request opts))

(defn put
  "Send the HTTP PUT `request` via `client`."
  [client request & [opts]]
  (send-method client :put request opts))

(defn trace
  "Send the HTTP TRACE `request` via `client`."
  [client request & [opts]]
  (send-method client :trace request opts))

#+clj
(extend-protocol IRequest
  clojure.lang.PersistentArrayMap
  (to-request [m]
    (check-request m))
  clojure.lang.PersistentHashMap
  (to-request [m]
    (check-request m))
  String
  (to-request [s]
    (check-request (parse-url s)))
  java.net.URI
  (to-request [uri]
    (to-request (str uri)))
  java.net.URL
  (to-request [url]
    (to-request (str url))))

#+cljs
(extend-protocol IRequest
  PersistentArrayMap
  (to-request [m]
    (check-request m))
  PersistentHashMap
  (to-request [m]
    (check-request m))
  string
  (to-request [s]
    (check-request (parse-url s)))
  goog.Uri
  (to-request [uri]
    (to-request (str uri))))
