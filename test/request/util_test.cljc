(ns request.util-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest is]])
            [request.util :as util]))

(deftest test-normalize-headers
  (are [headers expected]
      (= (:headers (util/normalize-headers {:headers headers})) expected)
    nil nil
    {} {}
    {:content-type "application/json"}
    {"content-type" "application/json"}))

(deftest test-request-method
  (are [request expected]
      (= (util/request-method request) expected)
    {} nil
    {:method :get} :get
    {:request-method :get} :get
    {:method :post :request-method :get} :get))

(deftest test-parse-content-type
  (are [s expected] (is (= (util/parse-content-type s) expected))
    nil nil
    "" nil
    "application/json"
    {:content-type "application/json"
     :content-type-params {}}
    " application/json "
    {:content-type "application/json"
     :content-type-params {}}
    "application/json; charset=UTF-8"
    {:content-type "application/json"
     :content-type-params {:charset "UTF-8"}}
    " application/json;  charset=UTF-8 "
    {:content-type "application/json"
     :content-type-params {:charset "UTF-8"}}
    "text/html; charset=ISO-8859-4"
    {:content-type "text/html"
     :content-type-params {:charset "ISO-8859-4"}}))
