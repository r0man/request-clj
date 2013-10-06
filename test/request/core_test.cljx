(ns request.core-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is are]]
                          [request.core :refer [defroutes]])
  (:require [request.core :as c]
            #+clj [request.core :refer [defroutes]]
            #+clj [clojure.test :refer :all]
            #+clj [clojure.core.async :refer [<!! go alts!]]
            #+cljs [cemerick.cljs.test :as t]))

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
   ;; :server-name "api.burningswell.dev"
   :server-port 80
   :as :auto})

(deftest test-expand-path
  (are [name opts expected]
    (is (= expected (c/expand-path (get routes name) {:path-params opts})))
    :continents {} "/continents"
    :continent {:id 1} "/continents/1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :update-continent {:id 1} "/continents/1"))

(deftest test-make-request-continent
  (let [request (c/make-request routes :continent {:path-params {:id 1}})]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-continents
  (let [request (c/make-request routes :continents)]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-make-request-create-continent
  (let [request (c/make-request routes :create-continent {:id 1 :name "Europe"})]
    (is (= {:id 1 :name "Europe"} (:body request)))
    (is (= :post (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-make-request-delete-continent
  (let [request (c/make-request routes :delete-continent {:path-params {:id 1}})]
    (is (= :delete (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-update-continent
  (let [request (c/make-request routes :update-continent {:id 1 :name "Europe"})]
    (is (= {:id 1 :name "Europe"} (:body request)))
    (is (= :put (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-make-request-override-defaults
  (let [default {:scheme :https :server-name "other.com" :server-port 8080}
        request (c/make-request routes :continents default)]
    (is (= :get (:method request) ))
    (is (= (:scheme default) (:scheme request) ))
    (is (= (:server-name default) (:server-name request) ))
    (is (= (:server-port default) (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-path-for-routes
  (are [name opts expected]
    (is (= expected ((c/path-for-routes routes) name opts)))
    :continents {} "/continents"
    :continents {:query-params {:a 1 :b 2}} "/continents?a=1&b=2"
    :continent {:id 1} "/continents/1"
    :continent {:path-params {:id 1}} "/continents/1"
    :continent {:path-params {:id 1} :query-params {:a 1}} "/continents/1?a=1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :delete-continent {:path-params {:id 1}} "/continents/1"
    :update-continent {:id 1} "/continents/1"
    :update-continent {:path-params {:id 1}} "/continents/1"))

(deftest test-url-for-routes
  (are [name opts expected]
    (is (= expected ((c/url-for-routes routes) name opts)))
    :continents {} "http://example.com/continents"
    :continent {:id 1} "http://example.com/continents/1"
    :continent {:path-params {:id 1}} "http://example.com/continents/1"
    :continent {:path-params {:id 1} :query-params {:a 1}} "http://example.com/continents/1?a=1"
    :create-continent {} "http://example.com/continents"
    :delete-continent {:id 1} "http://example.com/continents/1"
    :delete-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :update-continent {:id 1} "http://example.com/continents/1"
    :update-continent {:path-params {:id 1}} "http://example.com/continents/1"
    :continents {:server-port 80} "http://example.com/continents"
    :continents {:server-port 8080} "http://example.com:8080/continents"
    :continents {:scheme :https :server-port 443} "https://example.com/continents"
    :continents {:scheme :https :server-port 8080} "https://example.com:8080/continents"))

(deftest test-select-routes
  (is (= [{:path-params [:id], :path "/continents/:id", :route-name :continent, :method :get}
          {:path-params [], :path "/continents", :route-name :continents, :method :get}]
         (c/select-routes
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
            :path-params [:id]}]))))

#+clj
(deftest test-body
  (with-redefs
    [c/client
     (fn [request]
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (let [response (body :continents {:query-params {:query "Europe"}})]
      (is (= [{:id 1 :name "Europe"}] response))
      (is (= {:status 200 :headers {"Content-Type" "application/edn"}}
             (meta response))))))

#+clj
(deftest test-http
  (with-redefs
    [c/client
     (fn [request]
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (= {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}}
           (http :continents {:query-params {:query "Europe"}})))))

(comment
  (request :continents)
  (http<! :continents)
  (body :continents {:server-name "api.burningswell.dev"})
  (body :delete-continent {:server-name "api.burningswell.dev" :path-params {:id 1}})
  (body :delete-continent {:server-name "api.burningswell.dev" :path-params {:id 1}})
  (request :delete-continent {:id 1})
  (<!! (body<! :continents {:server-name "api.burningswell.dev"}))
  (<!! (http<! :continents {:server-name "api.burningswell.dev"})))
