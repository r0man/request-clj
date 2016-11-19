(ns request.middleware-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest is]])
            #?(:clj [clojure.test :refer :all])
            [request.middleware :as m]))

(deftest test-wrap-auth-token
  (is (= ((m/wrap-auth-token identity "secret") {})
         {:headers {"authorization" "Token secret"}}))
  (is (= ((m/wrap-auth-token identity) {:auth-token "secret"})
         {:headers {"authorization" "Token secret"}})))
