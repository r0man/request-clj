(ns request.util-test
  (:require [cemerick.cljs.test :as t]
            [request.util :refer [expand-path make-request path-for-routes url-for-routes select-routes]]
            [request.core-test :refer [routes]])
  (:require-macros [cemerick.cljs.test :refer [are is deftest]]))

(deftest test-expand-path
  (are [name opts expected]
    (is (= expected (expand-path (get routes name) {:path-params opts})))
    :continents {} "/continents"
    :continent {:id 1} "/continents/1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :update-continent {:id 1} "/continents/1"))

(deftest test-make-request-continent
  (let [request (make-request routes :continent {:path-params {:id 1}})]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-continents
  (let [request (make-request routes :continents)]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-make-request-create-continent
  (let [request (make-request routes :create-continent)]
    (is (= :post (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-make-request-delete-continent
  (let [request (make-request routes :delete-continent {:path-params {:id 1}})]
    (is (= :delete (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-update-continent
  (let [request (make-request routes :update-continent {:path-params {:id 1}})]
    (is (= :put (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-override-defaults
  (let [default {:scheme :https :server-name "other.com" :server-port 8080}
        request (make-request routes :continents default)]
    (is (= :get (:method request) ))
    (is (= (:scheme default) (:scheme request) ))
    (is (= (:server-name default) (:server-name request) ))
    (is (= (:server-port default) (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-path-for-routes
  (are [name opts expected]
    (is (= expected ((path-for-routes routes) name opts)))
    :continents {} "/continents"
    :continents {:query-params {:a 1 :b 2}} "/continents?a=1&b=2"
    :continent {:id 1} "/continents/1"
    :continent {:path-params {:id 1}} "/continents/1"
    :continent {:path-params {:id 1} :query-params {:a 1}} "/continents/1?a=1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :update-continent {:id 1} "/continents/1"))

(deftest test-url-for-routes
  (are [name opts expected]
    (is (= expected ((url-for-routes routes) name opts)))
    :continents {} "http://example.com/continents"
    :continent {:id 1} "http://example.com/continents/1"
    :continent {:path-params {:id 1}} "http://example.com/continents/1"
    :continent {:path-params {:id 1} :query-params {:a 1}} "http://example.com/continents/1?a=1"
    :create-continent {} "http://example.com/continents"
    :delete-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :update-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :continents {:server-port 80} "http://example.com/continents"
    :continents {:server-port 8080} "http://example.com:8080/continents"
    :continents {:scheme :https :server-port 443} "https://example.com/continents"
    :continents {:scheme :https :server-port 8080} "https://example.com:8080/continents"))

(deftest test-select-routes
  (let [routes [{:route-name :continents,
                 :path-re "/\\Qcontinents\\E",
                 :method :get,
                 :path "/continents",
                 :path-parts ["" "continents"],
                 :path-params []}
                {:route-name :continent,
                 :path-re "/\\Qcontinents\\E/([^/]+)",
                 :method :get,
                 :path-constraints {:id "([^/]+)"},
                 :path "/continents/:id",
                 :path-parts ["" "continents" :id],
                 :path-params [:id]}]]
    (is (= [{:path-params [:id], :path "/continents/:id", :route-name :continent, :method :get}
            {:path-params [], :path "/continents", :route-name :continents, :method :get}]
           (select-routes routes)))))
