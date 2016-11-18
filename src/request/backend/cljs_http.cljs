(ns request.backend.cljs-http
  (:require [cljs-http.client :as cljs-http]
            [request.middleware :as middleware]
            [request.core :refer [request]]))

(defn- coerce-request
  "Coerce `request`."
  [request]
  ((-> identity
       middleware/wrap-auth-token)
   request))

(defmethod request :cljs-http [client request]
  (cljs-http/request (coerce-request request)))
