(ns request.backend.clj-http
  (:require [clj-http.client :as clj-http]
            [request.middleware :as middleware]
            [request.core :refer [request]]))

(defn- coerce-request
  "Coerce `request`."
  [request]
  ((-> identity
       middleware/wrap-auth-token)
   request))

(defn- async-not-supported [request]
  (ex-info "The clj-http backend doesn't support asynchronous requests."
           {:request request}))

(defmethod request :clj-http [client request]
  (if (:async? request)
    (throw (async-not-supported request))
    (clj-http/request (coerce-request request))))
