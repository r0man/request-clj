(ns request.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? replace]]
            [no.en.core :refer [format-query-params format-url parse-url]]
            #+clj [clojure.pprint :refer [pprint]]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.core.async :refer [<! >! chan close! map< go put!]]
            #+clj [slingshot.slingshot :refer [try+]]
            #+clj [clj-http.client :as clj-http]
            #+cljs [cljs-http.client :as cljs-http]
            #+cljs [cljs.core.async :refer [<! >! chan close! map< put!]])
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]])
  #+cljs (:import goog.Uri))

(defprotocol IRequest
  (to-request [x] "Convert `x` into an HTTP request".))

(defn- check-request [request]
  (if-not (or (:uri request) (:url request))
    (throw (ex-info "HTTP request is missing :uri or :url." {:request request}))
    (merge {:accept "application/edn"
            :as :auto
            :content-type "application/edn"
            :method :get}
           request)))

(defn with-meta-resp [resp]
  (let [body (:body resp)]
    (if (or (map? body)
            (sequential? body))
      (with-meta body (dissoc resp :body))
      body)))

(defn wrap-edn-body [client]
  (fn [request]
    (if (:edn-body request)
      (-> (dissoc request :edn-body)
          (assoc :body (pr-str (:edn-body request)))
          (assoc-in [:headers "Content-Type"] "application/edn")
          (client))
      (client request))))

#+clj
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
        (clj-http/with-connection-pool {}
          (paginate request 1 (or per-page 100)))))))

(def client
  #+clj
  (->  #'clj-http/request
       (wrap-edn-body))
  #+cljs
  (->  cljs-http/request
       (wrap-edn-body)))

(defn http
  "Make a HTTP request and return the response."
  [request]
  #+clj
  (->> (to-request request)
       (merge {:throw-exceptions false})
       (client))
  #+cljs
  (throw (ex-info "Not implemented in JavaScript." {})))

(defn http!
  "Make a HTTP request and return the response."
  [request]
  #+clj
  (->> (to-request request)
       (merge {:throw-exceptions true})
       (client))
  #+cljs
  (throw (ex-info "Not implemented in JavaScript." {})))

(defn http<
  "Make a HTTP request and return a core.async channel."
  [request]
  #+clj
  (let [channel (chan)
        request (merge {:throw-exceptions false} (to-request request))]
    (check-request request)
    (go (try+ (>! channel (client request))
              (finally (close! channel))))
    channel)
  #+cljs
  (client request))

(defmacro http<!
  "Make a HTTP request and return a core.async channel."
  [request]
  `(let [channel# (request.core/http< ~request)]
     #+clj (clojure.core.async/<! channel#)
     #+cljs (cljs.core.async/<! channel#)))

#+clj
(extend-protocol IRequest
  clojure.lang.PersistentArrayMap
  (to-request [m]
    (check-request m))
  clojure.lang.PersistentHashMap
  (to-request [m]
    (check-request m))
  String
  (to-request [s]
    (check-request (parse-url s)))
  java.net.URI
  (to-request [uri]
    (to-request (str uri)))
  java.net.URL
  (to-request [url]
    (to-request (str url))))

#+cljs
(extend-protocol IRequest
  PersistentArrayMap
  (to-request [m]
    (check-request m))
  PersistentHashMap
  (to-request [m]
    (check-request m))
  string
  (to-request [s]
    (check-request (parse-url s)))
  goog.Uri
  (to-request [uri]
    (to-request (str uri))))
