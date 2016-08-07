(ns request.backend.httpkit-test
  (:require [clojure.test :refer :all]
            [request.backend.httpkit :refer :all]))

(deftest test-coerce-request
  (let [request (coerce-request
                 {:as :auto
                  :content-type "application/json"
                  :form-params {:a 1}
                  :method :post
                  :url "http://httpbin.org/headers"})]
    (is (= (:method request) :post))
    (is (= (:request-method request) :post))
    (is (= (:body request) "{\"a\":1}"))
    (is (= (get-in request [:headers "content-type"]) "application/json"))))

(deftest test-get
  (let [response (request-sync
                  {:url "http://httpbin.org/get"})]
    (is (= (:status response) 200))
    (is (string? (:body response)))))

(deftest test-post
  (let [response (request-sync
                  {:method :post
                   :url "http://httpbin.org/post"})]
    (is (= (:status response) 200))
    (is (string? (:body response)))))

(deftest test-post-json
  (let [response (request-sync
                  {:as :auto
                   :content-type "application/json"
                   :form-params {:a 1}
                   :method :post
                   :url "http://httpbin.org/post"})]
    (is (= (:status response) 200))
    (is (= (-> response :body :data) "{\"a\":1}"))))

(deftest test-headers
  (let [response (request-sync
                  {:as :auto
                   :headers {"content-type" "application/json"}
                   :method :get
                   :url "http://httpbin.org/headers"})]
    (is (= (:status response) 200))
    (is (= (:body response)
           {:headers
            {:Accept-Encoding "gzip, deflate",
             :Content-Length "0",
             :Content-Type "application/json",
             :Host "httpbin.org",
             :User-Agent "http-kit/2.0"}}))))

(deftest test-headers-are-lower-case
  (let [response (request-sync {:url "http://httpbin.org/get"})]
    (is (= (:status response) 200))
    (is (every? string? (keys (:headers response))))))
