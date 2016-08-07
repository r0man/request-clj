(ns request.backend.cljs-http
  (:require [cljs-http.client :as cljs-http]
            [request.core :refer [request]]))

(defmethod request :cljs-http [client request]
  (cljs-http/request request))
