(ns request.platform
  (:require [clj-http.client :as http]
            [clj-http.client :refer [generate-query-string]]
            [clojure.core.async :refer [<! chan close! go put!]]
            [request.util :refer [make-request unpack-response]]
            [slingshot.slingshot :refer [try+]]))

(defn query-string
  "Generate a url encoded query string from `m`."
  [m] (generate-query-string m))

(defn wrap-pagination [client & [per-page]]
  (letfn [(paginate [request & [page per-page]]
            (update-in
             (-> request
                 (assoc-in [:query-params :page] page)
                 (assoc-in [:query-params :per-page] per-page)
                 (client))
             [:body]
             #(if (sequential? %1)
                (lazy-seq
                 (if-not (empty? %1)
                   (concat %1 (:body (paginate request (inc page) per-page)))
                   %1))
                %1)))]
    (fn [request]
      (if (or (= :get (:method request))
              (-> request :query-params :page))
        (client request)
        (http/with-connection-pool {}
          (paginate request 1 (or per-page 100)))))))

(def client (wrap-pagination #'http/request))

(defn http
  "Make a HTTP request and return the response."
  [routes name & opts]
  (client (apply make-request routes name opts)))

(defn http<!
  "Make a HTTP request and return a core.async channel."
  [routes name & opts]
  (let [channel (chan)]
    (go (try+ (->> (apply make-request routes name opts)
                   (client)
                   (>! channel))
              (catch map? response
                (>! channel response))
              (catch Object _
                (>! channel (:throwable &throw-context)))
              (finally (close! channel))))
    channel))

(defn body
  "Make a HTTP request and return the body of response."
  [routes name & opts]
  (unpack-response (apply http routes name opts)))

(defn body<!
  "Make a HTTP request and return the body in a core.async channel."
  [routes name & opts]
  (let [channel (chan)]
    (go (let [response (<! (apply http<! routes name opts))]
          (put! channel (unpack-response response))
          (close! channel)))
    channel))
