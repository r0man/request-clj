(ns request.backend.httpkit
  (:require [clj-http.client :as clj-http]
            [clojure.core.async :as async]
            [no.en.core :refer [format-url]]
            [org.httpkit.client :as http]
            [request.core :refer [request]]
            [request.util :as util]))

(defn coerce-request
  "Coerce `request` into a clj-http compatible format."
  [request]
  (-> ((-> identity
           clj-http/wrap-content-type
           clj-http/wrap-form-params
           clj-http/wrap-method
           clj-http/wrap-accept-encoding
           clj-http/wrap-accept)
       (util/normalize-headers request))
      (dissoc :query-params :query-string)
      (assoc :as :stream
             :method (util/request-method request)
             :url (or (:url request) (format-url request)))))

(defn coerce-response
  "Coerce `response` into a clj-http compatible format."
  [request response]
  (clj-http/coerce-response-body
   request (-> (util/normalize-headers response)
               (assoc :as :auto))))

(defn request-async
  "Make an asynchronous HTTP `request`."
  [request]
  (let [channel (async/chan)]
    (http/request
     (coerce-request request)
     (fn [{:keys [status headers body error opts] :as response}]
       (try (let [response (coerce-response request response)]
              (async/put! channel response))
            (catch Exception exception
              (.printStackTrace exception)
              (async/put! channel exception))
            (finally
              (async/close! channel)))))
    channel))

(defn request-sync
  "Make a synchronous HTTP `request`."
  [request]
  (async/<!! (request-async request)))

(defmethod request :httpkit [client request]
  (if (:async? request)
    (request-async request)
    (request-sync request)))
