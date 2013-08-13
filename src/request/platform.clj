(ns request.platform
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! <! <!! chan close! go]]
            [request.util :refer [make-request unpack-response]]))

(defn http [routes name & opts]
  (let [channel (chan 1)]
    (go (->> (apply make-request routes name opts)
             (http/request)
             (unpack-response)
             (>! channel))
        (close! channel))
    channel))

(defn http<! [routes name & opts]
  (<! (apply http routes name opts)))

(defn http<!! [routes name & opts]
  (<! (apply http routes name opts)))
