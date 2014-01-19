(ns request.core-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is are]])
  (:require [request.core :as core]
            #+clj [clojure.edn :as edn]
            #+clj [clojure.test :refer :all]
            #+clj [clojure.core.async :refer [<!! go alts!]]
            #+clj [clj-http.client :as clj-http]
            #+cljs [cljs-http.client :as clj-http]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:import goog.Uri))

(deftest test-to-request
  (is (= {:url "http://api.burningswell.com/continents", :method :get}
         (core/to-request {:url "http://api.burningswell.com/continents"})))
  (is (= (core/to-request "http://api.burningswell.com/continents")
         #+clj (core/to-request (java.net.URL. "http://api.burningswell.com/continents"))
         #+clj (core/to-request (java.net.URI. "http://api.burningswell.com/continents"))
         #+cljs (core/to-request (goog.Uri. "http://api.burningswell.com/continents")))))

(deftest test-wrap-edn-body
  (is (= {:headers {"Content-Type" "application/edn"}, :body "{:a 1, :b 2}"}
         ((core/wrap-edn-body identity) {:edn-body {:a 1 :b 2}}))))

#+clj
(deftest test-http
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (= {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}}
           (core/http "http://example.com/continents?query=Europe"))))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :post (:method request)))
       (is (= "http://example.com/continents" (:url request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (= {:status 201, :body "{:name \"Europe\"}", :headers {"content-type" "application/edn"}}
           (core/http {:method :post
                       :url "http://example.com/continents"
                       :as :auto
                       :edn-body {:name "Europe"}})))))

#+clj
(deftest test-http!
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (= {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}}
           (core/http! "http://example.com/continents?query=Europe"))))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= :post (:method request)))
       (is (= "http://example.com/continents" (:url request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (= {:status 201, :body "{:name \"Europe\"}", :headers {"content-type" "application/edn"}}
           (core/http {:method :post
                       :url "http://example.com/continents"
                       :as :auto
                       :edn-body {:name "Europe"}})))))

(deftest test-http<
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (core/http< "http://example.com/continents?query=Europe")))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= :post (:method request)))
       (is (= "http://example.com/continents" (:url request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (core/http< {:method :post
                     :url "http://example.com/continents"
                     :as :auto
                     :edn-body {:name "Europe"}}))))

(comment
  (core/http "http://api.burningswell.com/continents")
  (core/http! "http://api.burningswell.com/continents")
  (<!! (core/http< "http://api.burningswell.com/continents"))
  (go (prn (core/http<! "http://api.burningswell.com/continents"))))
