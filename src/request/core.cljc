(ns request.core
  (:refer-clojure :exclude [get])
  (:require [no.en.core :refer [parse-url]]
            [routes.core :as routes])
  #?(:cljs (:import goog.Uri)))

(defrecord Client [backend pool router])

(defprotocol IRequest
  (to-request [x] "Convert `x` into a HTTP request map."))

(defn- check-request [request]
  (if-not (or (:url request) (:server-name request) )
    (throw (ex-info "HTTP request is missing :uri or :url." {:request request}))
    request))

(defn wrap-auth-token
  "Middleware converting the :auth-token option into an Authorization header."
  [client & [token]]
  (fn [req]
    (if-let [auth-token (or (:auth-token req) token)]
      (client (-> req (dissoc :auth-token)
                  (assoc-in [:headers "authorization"]
                            (str "Token " auth-token))))
      (client req))))

(defn new-client
  "Return a new HTTP client using `config`."
  [& [config]]
  (-> (merge
       {:as :auto
        :backend #?(:clj :clj-http :cljs :cljs-http)
        :throw-exceptions false}
       config)
      (map->Client)))

(defn request-for
  "Return the HTTP request for `route`."
  [client route & args]
  (if (keyword? route)
    (if-let [router (:router client)]
      (or (apply routes/request-for router client route args)
          (throw (ex-info "Can't resolve route." {:route route})))
      (throw (ex-info "No routes defined for client." {:route route})))
    (merge (to-request route) client (first args))))

(defmulti request
  "Send the HTTP `request` via `client`."
  (fn [client request] (:backend client)))

(defmethod request :default [client request]
  (throw (ex-info (str "Can't send HTTP request. Backend \""
                       (-> client :backend name) "\" not registered.")
                  {:request request})))

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
  [client & args]
  (apply send-method client :connect args))

(defn delete
  "Send the HTTP DELETE `request` via `client`."
  [client & args]
  (apply send-method client :delete args))

(defn get
  "Send the HTTP GET `request` via `client`."
  [client & args]
  (apply send-method client :get args))

(defn head
  "Send the HTTP HEAD `request` via `client`."
  [client & args]
  (apply send-method client :head args))

(defn options
  "Send the HTTP OPTIONS `request` via `client`."
  [client & args]
  (apply send-method client :options args))

(defn patch
  "Send the HTTP PATCH `request` via `client`."
  [client & args]
  (apply send-method client :patch args))

(defn post
  "Send the HTTP POST `request` via `client`."
  [client & args]
  (apply send-method client :post args))

(defn put
  "Send the HTTP PUT `request` via `client`."
  [client & args]
  (apply send-method client :put args))

(defn trace
  "Send the HTTP TRACE `request` via `client`."
  [client & args]
  (apply send-method client :trace args))

#?(:clj
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
       (to-request (str url)))))

#?(:cljs
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
       (to-request (str uri)))))
