(ns request.core-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is are]]
                          [request.core :refer [defroutes]])
  (:require [request.core :as http]
            #+clj [request.core :refer [defroutes]]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.test :refer :all]
            #+clj [clj-http.core :as clj-http]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:import goog.Uri))

(defroutes my-routes
  [{:route-name :continents,
    :path-re #"/continents",
    :method :get,
    :path "/continents",
    :path-parts ["" "continents"],
    :path-params []}
   {:route-name :continent,
    :path-re #"/continents/([^/]+)",
    :method :get,
    :path-constraints {:id "([^/]+)"},
    :path "/continents/:id",
    :path-parts ["" "continents" :id],
    :path-params [:id]}]
  :scheme :http
  :server-name "example.com"
  :server-port 80)

(def client
  (new-client
   {:scheme :http
    :server-name "example.com"
    :server-port 80}))

(deftest test-to-request
  (is (= {:url "http://api.burningswell.com/continents"}
         (http/to-request {:url "http://api.burningswell.com/continents"})))
  (is (= (http/to-request "http://api.burningswell.com/continents")
         #+clj (http/to-request (java.net.URL. "http://api.burningswell.com/continents"))
         #+clj (http/to-request (java.net.URI. "http://api.burningswell.com/continents"))
         #+cljs (http/to-request (goog.Uri. "http://api.burningswell.com/continents")))))

(deftest test-request
  (let [request (http/request client :continents)]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/continents" (:uri request)))
    (is (= {} (:query-params request)))
    (is (= :auto (:as request))))
  (let [request (http/request client :continents {:query-params {:query "Europe"}})]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/continents" (:uri request)))
    (is (= {:query "Europe"} (:query-params request)))
    (is (= :auto (:as request))))
  (let [request (http/request client "http://example.com/continents?query=Europe")]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/continents" (:uri request)))
    (is (= {:query "Europe"} (:query-params request)))
    (is (= :auto (:as request)))
    (is (= #+clj (http/request client (java.net.URL. "http://example.com/continents?query=Europe"))
           #+clj (http/request client (java.net.URI. "http://example.com/continents?query=Europe"))
           #+cljs (http/request client (goog.Uri. "http://example.com/continents?query=Europe"))
           request))))

#+clj
(deftest test-get
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :get (:request-method request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= "/continents" (:uri request)))
       (is (= "query=Europe" (:query-string request)))
       (is (= :auto (:as request)))
       {:status 200
        :body (java.io.ByteArrayInputStream. (.getBytes (pr-str {:a 1 :b 2})))
        :headers {"content-type" "application/edn"}})]
    (let [response (http/get client :continents {:query-params {:query "Europe"}})]
      (is (= (:status response) 200))
      (is (= (:body response) {:a 1 :b 2}))
      (is (= (:headers response) {"content-type" "application/edn"})))))

(clojure.pprint/pprint (http/request client :continents {:query-params {:query "Europe"}}))
