(ns request.platform
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! chan close! go]]
            [request.util :refer [make-request]]))

(defn request [routes name & opts]
  (let [channel (chan 1)]
    (go (->> (apply make-request routes name opts)
             (http/request)
             (>! channel))
        (close! channel))
    channel))
