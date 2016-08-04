(ns request.util
  (:require [clojure.string :as str]
            [no.en.core :refer [map-keys]] ))

(defn normalize-headers
  "Normalize the :headers in `request`."
  [request]
  (->> #(when % (map-keys (comp str/lower-case name) %))
       (update request :headers )))

(defn request-method
  "Return the :request-method or :method from `request`."
  [request]
  (or (:request-method request) (:method request)))

(def content-type-regex
  "The regular expression for the Content-Type header."
  #"\s*(([^/]+)/([^ ;]+))\s*(\s*;.*)?")

(defn parse-content-type
  "Parse `s` as an RFC 2616 media type."
  [s]
  (when-let [m (re-matches content-type-regex (str s))]
    {:content-type (nth m 1)
     :content-type-params
     (->> (str/split (str (nth m 4)) #"\s*;\s*")
          (identity)
          (remove str/blank?)
          (map #(str/split % #"="))
          (mapcat (fn [[k v]] [(keyword (str/lower-case k)) (str/trim v)]))
          (apply hash-map))}))
