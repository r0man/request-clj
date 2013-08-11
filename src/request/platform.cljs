(ns request.platform
  (:require [cljs-http.client :as client]
            [request.util :refer [make-request]]))

(defn request [routes name & opts]
  (-> (apply make-request routes name opts)
      (client/request)))
