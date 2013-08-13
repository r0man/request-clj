(ns request.platform
  (:require [cljs-http.client :as client]
            [cljs.core.async :refer [<! chan close! put!]]
            [request.util :refer [make-request unpack-response]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

(defn http [routes name & opts]
  (let [channel (chan)
        response (-> (apply make-request routes name opts)
                     (client/request))
        enqueue (fn [response]
                  (put! channel (unpack-response response))
                  (close! channel))]
    (client/on-success response enqueue)
    (client/on-error response enqueue)
    channel))

(defn http<! [routes name & opts]
  (<! (apply http routes name opts)))

(defn http<<! [routes name & opts]
  (throw js/Error "Not implemented."))
