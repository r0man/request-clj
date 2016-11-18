(ns request.middleware-test
  (:require [clojure.test :refer [are deftest is]]
            [request.middleware :as m]))

(deftest test-wrap-auth-token
  (is (= ((m/wrap-auth-token identity "secret") {})
         {:headers {"authorization" "Token secret"}}))
  (is (= ((m/wrap-auth-token identity) {:auth-token "secret"})
         {:headers {"authorization" "Token secret"}})))
