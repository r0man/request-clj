(ns request.platform
  (:require [cljs-http.client :as http]
            [request.util :refer [make-request]]))

(defn request [routes name & opts]
  (-> (apply make-request routes name opts)
      (http/request)))
