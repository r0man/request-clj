(ns request.platform
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as client]
            [cljs.core.async :refer [<! chan close! put!]]
            [request.util :refer [make-request unpack-response]]))

(defn http
  "Make a HTTP request and return the response."
  [routes name & opts]
  (throw js/Error "Not implemented on JavaScript runtime."))

(defn http<!
  "Make a HTTP request and return a core.async channel."
  [routes name & opts]
  (client/request (apply make-request routes name opts)))

(defn body
  "Make a HTTP request and return the body of the response."
  [routes name & opts]
  (throw js/Error "Not implemented on JavaScript runtime."))

(defn body<!
  "Make a HTTP request and return the body in a core.async channel."
  [routes name & opts]
  (let [channel (chan)]
    (go (let [response (<! (apply http<! routes name opts))]
          (put! channel (unpack-response response))
          (close! channel)))
    channel))
