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

(defn request
  "Send the HTTP `request` via `client`."
  [client request]
  (let [request (to-request request)]
    ((:backend client) (merge (into {} client) request))))

(defn request-for
  "Return the HTTP request for `route`."
  [client route & args]
  (if (keyword? route)
    (if-let [router (:router client)]
      (or (apply routes/request-for router client route args)
          (throw (ex-info "Can't resolve route." {:route route})))
      (throw (ex-info "No routes defined for client." {:route route})))
    (merge (to-request route) client (first args))))

(defn- send-method
  "Send the HTTP `request` as `verb` via `client`."
  [client verb & args]
  (->> (assoc (apply request-for client args)
              :method verb
              :request-method verb)
       (request client)))

(defmacro defroutes
  "Define routes and a client."
  [name & routes]
  `(do (routes.core/defroutes ~name
         ~@(filter vector? routes))
       (defn ~'new-client [& [~'config]]
         (let [config# (-> ~(apply merge (filter map? routes))
                           (merge ~'config)
                           (assoc :router ~name))]
           (assert (:server-name config#) "No server name given!")
           (request.core/new-client config#)))))

;; HTTP methods

(defn connect
  "Send the HTTP CONNECT `request` via `client`."
  [client  & args]
  (apply send-method client :connect args))

(defn delete
  "Send the HTTP DELETE `request` via `client`."
  [client  & args]
  (apply send-method client :delete args))

(defn get
  "Send the HTTP GET `request` via `client`."
  [client  & args]
  (apply send-method client :get args))

(defn head
  "Send the HTTP HEAD `request` via `client`."
  [client  & args]
  (apply send-method client :head args))

(defn options
  "Send the HTTP OPTIONS `request` via `client`."
  [client  & args]
  (apply send-method client :options args))

(defn patch
  "Send the HTTP PATCH `request` via `client`."
  [client  & args]
  (apply send-method client :patch args))

(defn post
  "Send the HTTP POST `request` via `client`."
  [client  & args]
  (apply send-method client :post args))

(defn put
  "Send the HTTP PUT `request` via `client`."
  [client  & args]
  (apply send-method client :put args))

(defn trace
  "Send the HTTP TRACE `request` via `client`."
  [client  & args]
  (apply send-method client :trace args))

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
