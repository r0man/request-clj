(ns request.core-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is are]]
                          [request.core :refer [defroutes]])
  (:require [request.core :as http]
            #+clj [request.core :refer [defroutes]]
            #+clj [clojure.test :refer :all]
            #+clj [clj-http.core :as clj-http]
            #+cljs [cemerick.cljs.test :as t])
  #+clj (:import [java.net URL URI])
  #+cljs (:import goog.Uri))

(def server
  {:scheme :http
   :server-name "example.com"
   :server-port 80})

(def spain
  {:id 1 :iso-3166-1-alpha-2 "ES" :name "Spain"})

(def mundaka
  {:id 2 :name "Mundaka"})

(defroutes my-routes
  ["/countries" :countries]
  ["/countries/:id-:name" :country]
  ["/countries/:id-:name/spots" :spots-in-country]
  ["/countries/:id-:name/spots/:id-:name" :spot-in-country]
  ["/spots" :spots]
  ["/spots/:id-:name" :spot])

(def client (new-client server))

(deftest test-client
  (is (map? (:router client)))
  (is (= (:scheme client) :http))
  (is (= (:server-name client) "example.com"))
  (is (= (:server-port client) 80)))

(deftest test-to-request
  (is (= {:url "http://api.burningswell.com/countries"}
         (http/to-request {:url "http://api.burningswell.com/countries"})))
  (is (= (http/to-request "http://api.burningswell.com/countries")
         #+clj (http/to-request (URL. "http://api.burningswell.com/countries"))
         #+clj (http/to-request (URI. "http://api.burningswell.com/countries"))
         #+cljs (http/to-request (Uri. "http://api.burningswell.com/countries")))))

(deftest test-request-for
  (let [request (http/request-for client :countries)]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/countries" (:uri request)))
    (is (= nil (:query-params request)))
    (is (= :auto (:as request))))
  (let [request (http/request-for client :country spain {:query-params {:sort "asc"}})]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/countries/1-Spain" (:uri request)))
    (is (= {:sort "asc"} (:query-params request)))
    (is (= :auto (:as request))))
  (let [request (http/request-for client :countries {:query-params {:query "Europe"}})]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/countries" (:uri request)))
    (is (= {:query "Europe"} (:query-params request)))
    (is (= :auto (:as request))))
  (let [request (http/request-for client "http://example.com/countries?query=Europe")]
    (is (= false (:throw-exceptions request)))
    (is (= :http (:scheme request)))
    (is (= "example.com" (:server-name request)))
    (is (= 80 (:server-port request)))
    (is (= "/countries" (:uri request)))
    (is (= {:query "Europe"} (:query-params request)))
    (is (= :auto (:as request)))
    (is (= #+clj (http/request-for client (URL. "http://example.com/countries?query=Europe"))
           #+clj (http/request-for client (URI. "http://example.com/countries?query=Europe"))
           #+cljs (http/request-for client (Uri. "http://example.com/countries?query=Europe"))
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
       (is (= "/countries" (:uri request)))
       (is (= "query=Europe" (:query-string request)))
       (is (= :auto (:as request)))
       {:status 200
        :body (java.io.ByteArrayInputStream. (.getBytes (pr-str {:a 1 :b 2})))
        :headers {"content-type" "application/edn"}})]
    (let [response (http/get client :countries {:query-params {:query "Europe"}})]
      (is (= (:status response) 200))
      (is (= (:body response) {:a 1 :b 2}))
      (is (= (:headers response) {"content-type" "application/edn"}))))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :get (:request-method request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= "/countries/1-Spain" (:uri request)))
       (is (= "sort=asc" (:query-string request)))
       (is (= :auto (:as request)))
       {:status 200
        :body (java.io.ByteArrayInputStream. (.getBytes (pr-str {:a 1 :b 2})))
        :headers {"content-type" "application/edn"}})]
    (let [response (http/get client :country spain {:query-params {:sort "asc"}})]
      (is (= (:status response) 200))
      (is (= (:body response) {:a 1 :b 2}))
      (is (= (:headers response) {"content-type" "application/edn"})))))
