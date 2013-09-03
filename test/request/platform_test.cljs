(ns request.platform-test
  (:require [cemerick.cljs.test :as t]
            [request.platform :refer [query-string]])
  (:require-macros [cemerick.cljs.test :refer [are is deftest]]))

(deftest test-query-string
  ;; TODO: %20 or + ?
  (is (= "a=1&b=%20" (query-string {:a 1 :b " "}))))
