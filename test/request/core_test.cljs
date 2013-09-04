(ns request.core-test
  (:require [cemerick.cljs.test :as t]
            [cljs.core.async :refer [<! chan close!]]
            [request.util :refer [path-for-routes url-for-routes]])
  (:require-macros [cemerick.cljs.test :refer [are is deftest]]
                   [cljs.core.async.macros :refer [go alt!]]
                   [request.core :refer [defroutes]]))

(defroutes routes
  [{:route-name :continents,
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
    :path-params [:id]}
   {:route-name :create-continent,
    :path-re "/\\Qcontinents\\E",
    :method :post,
    :path "/continents",
    :path-parts ["" "continents"],
    :path-params []}
   {:route-name :delete-continent,
    :path-re "/\\Qcontinents\\E/([^/]+)",
    :method :delete,
    :path-constraints {:id "([^/]+)"},
    :path "/continents/:id",
    :path-parts ["" "continents" :id],
    :path-params [:id]}
   {:route-name :update-continent,
    :path-re "/\\Qcontinents\\E/([^/]+)",
    :method :put,
    :path-constraints {:id "([^/]+)"},
    :path "/continents/:id",
    :path-parts ["" "continents" :id],
    :path-params [:id]}]
  {:scheme :http
   :server-name "example.com"
   :server-port 80})

(deftest test-path-for
  (are [name opts expected]
    (is (= expected (path-for name {:path-params opts})))
    :continents {} "/continents"
    :continent {:id 1} "/continents/1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :update-continent {:id 1} "/continents/1"))

(deftest test-url-for
  (are [name opts expected]
    (is (= expected (url-for name opts)))
    :continents {} "http://example.com/continents"
    :continent {:path-params {:id 1}} "http://example.com/continents/1"
    :create-continent {} "http://example.com/continents"
    :delete-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :update-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :continents {:server-port 80} "http://example.com/continents"
    :continents {:server-port 8080} "http://example.com:8080/continents"
    :continents {:scheme :https :server-port 443} "https://example.com/continents"
    :continents {:scheme :https :server-port 8080} "https://example.com:8080/continents"))

(comment
  (url-for :continents)
  (go (prn (<! (request :continents)))))
