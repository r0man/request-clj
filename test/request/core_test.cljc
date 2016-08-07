(ns request.core-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest is]])
            #?(:clj [clojure.test :refer :all])
            #?(:clj [clj-http.core :as clj-http])
            #?(:cljs [cljs-http.core :as cljs-http])
            #?(:clj [request.backend.clj-http])
            #?(:clj [request.backend.httpkit])
            [request.core :as http #?(:clj :refer :cljs :refer-macros) [defroutes]])
  #?(:clj (:import [java.net URL URI]))
  #?(:cljs (:import goog.Uri)))

(def server
  {:scheme :https
   :server-name "other.com"
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
  ["/spots/:id-:name" :spot]
  {:scheme :http
   :server-name "example.com"
   :server-port 80})

(def client (new-client))

(deftest test-client
  (let [client (new-client server)]
    (is (map? (:router client)))
    (is (= (:scheme client) :https))
    (is (= (:server-name client) "other.com"))
    (is (= (:server-port client) 80))))

(deftest test-to-request
  (is (= {:url "http://api.burningswell.com/countries"}
         (http/to-request {:url "http://api.burningswell.com/countries"})))
  (is (= (http/to-request "http://api.burningswell.com/countries")
         #?(:clj (http/to-request (URL. "http://api.burningswell.com/countries")))
         #?(:clj (http/to-request (URI. "http://api.burningswell.com/countries")))
         #?(:cljs (http/to-request (Uri. "http://api.burningswell.com/countries")))))
  (is (= (http/to-request "http://example.com")
         {:scheme :http
          :server-name "example.com"
          :server-port 80})))

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
    (is (= #?(:clj (http/request-for client (URL. "http://example.com/countries?query=Europe")))
           #?(:clj (http/request-for client (URI. "http://example.com/countries?query=Europe")))
           #?(:cljs (http/request-for client (Uri. "http://example.com/countries?query=Europe")))
           request))))

#?(:clj
   (deftest test-get-countries
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
         (is (= (:headers response) {"content-type" "application/edn"}))))))

#?(:clj
   (deftest test-get-country
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
         (is (= (:headers response) {"content-type" "application/edn"}))))))

#?(:clj
   (deftest test-get-url
     (with-redefs
       [clj-http/request
        (fn [request]
          (is (= false (:throw-exceptions request)))
          (is (= :get (:request-method request)))
          (is (= :http (:scheme request)))
          (is (= "example.com" (:server-name request)))
          (is (= 80 (:server-port request)))
          (is (= "/" (:uri request)))
          (is (= "sort=asc" (:query-string request)))
          (is (= :auto (:as request)))
          {:status 200
           :body (java.io.ByteArrayInputStream. (.getBytes (pr-str {:a 1 :b 2})))
           :headers {"content-type" "application/edn"}})]
       (let [response (http/get client "http://example.com/" {:query-params {:sort "asc"}})]
         (is (= (:status response) 200))
         (is (= (:body response) {:a 1 :b 2}))
         (is (= (:headers response) {"content-type" "application/edn"}))))))

(deftest test-wrap-auth-token
  (is (= ((http/wrap-auth-token identity "secret") {})
         {:headers {"authorization" "Token secret"}}))
  (is (= ((http/wrap-auth-token identity) {:auth-token "secret"})
         {:headers {"authorization" "Token secret"}})))

#?(:clj
   (deftest test-get
     (doseq [client (map #(http/new-client {:backend %}) [:clj-http :httpkit])]
       (let [url "http://httpbin.org/get"
             response (http/get client url)]
         (is (= (:status response) 200))
         (is (map? (:body response)))
         (is (= (-> response :body :url) url))))))

#?(:clj
   (deftest test-post-json
     (doseq [client (map #(http/new-client {:backend %}) [:clj-http :httpkit])]
       (let [url "http://httpbin.org/post"
             response (http/post
                       client url
                       {:content-type "application/json"
                        :form-params {:a 1}})]
         (is (= (:status response) 200))
         (is (map? (:body response)))
         (is (= (-> response :body :url) url))
         (is (= (-> response :body :data) "{\"a\":1}"))))))
