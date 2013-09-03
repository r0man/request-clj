(ns request.platform-test
  (:require [clojure.test :refer :all]
            [request.platform :refer :all]))

(deftest test-query-string
  (is (= "a=1&b=+" (query-string {:a 1 :b " "}))))
