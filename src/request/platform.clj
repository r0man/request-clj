(ns request.platform
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [>! <! <!! chan close! go]]
            [request.util :refer [make-request unpack-response]]))

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

(def client (wrap-pagination http/request))

(defn http [routes name & opts]
  (let [channel (chan 1)]
    (go (->> (apply make-request routes name opts)
             (client)
             (unpack-response)
             (>! channel))
        (close! channel))
    channel))

(defn http<!! [routes name & opts]
  (<!! (apply http routes name opts)))
