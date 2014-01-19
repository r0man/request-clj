(ns request.routes-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is are]]
                          [request.routes :refer [defroutes]])
  (:require [request.core :as core]
            [request.routes :as routes]
            #+clj [clojure.edn :as edn]
            #+clj [request.routes :refer [defroutes]]
            #+clj [clojure.test :refer :all]
            #+clj [clojure.core.async :refer [<!! go alts!]]
            #+clj [clj-http.client :as clj-http]
            #+cljs [cljs-http.client :as clj-http]
            #+cljs [cemerick.cljs.test :as t]))

(defroutes routes
  [{:route-name :continents,
    :path-re #"/continents",
    :method :get,
    :path "/continents",
    :path-parts ["" "continents"],
    :path-params []}
   {:route-name :continent,
    :path-re #"/continents/([^/]+)",
    :method :get,
    :path-constraints {:id "([^/]+)"},
    :path "/continents/:id",
    :path-parts ["" "continents" :id],
    :path-params [:id]}
   {:route-name :create-continent,
    :path-re #"/continents",
    :method :post,
    :path "/continents",
    :path-parts ["" "continents"],
    :path-params []}
   {:route-name :delete-continent,
    :path-re #"/continents/([^/]+)",
    :method :delete,
    :path-constraints {:id "([^/]+)"},
    :path "/continents/:id",
    :path-parts ["" "continents" :id],
    :path-params [:id]}
   {:route-name :update-continent,
    :path-re #"/continents/([^/]+)",
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

(deftest test-assoc-route
  (let [routes (routes/assoc-route {} :continents #"/continents")
        route (:continents routes)]
    (is (= :get (:method route)))
    (is (= :continents (:route-name route)))
    #+clj (is (= "/continents" (str (:path-re route))))
    #+cljs (is (= "/\\/continents/" (str (:path-re route)))))
  (let [routes (routes/assoc-route {} :create-continent #"/continents" {:method :post})
        route (:create-continent routes)]
    (is (= :post (:method route)))
    (is (= :create-continent (:route-name route)))
    #+clj (is (= "/continents" (str (:path-re route))))
    #+cljs (is (= "/\\/continents/" (str (:path-re route))))))

(deftest test-expand-path
  (are [name opts expected]
    (is (= expected (routes/expand-path (get routes name) {:path-params opts})))
    :continents {} "/continents"
    :continent {:id 1} "/continents/1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :update-continent {:id 1} "/continents/1"))

(deftest test-resolve-route-empty-params
  (let [request (routes/resolve-route routes :continent {})]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/:id" (:uri request)))))

(deftest test-resolve-route-not-existing
  (is (nil? (routes/resolve-route routes :not-existing)))
  (let [request {:method :get :url "http://example.com"}]
    (is (= request (routes/resolve-route routes :not-existing request)))))

(deftest test-resolve-route-without-routes
  (is (nil? (routes/resolve-route nil)))
  (let [request {:method :get :url "http://example.com"}]
    (is (= request (routes/resolve-route request)))))

(deftest test-resolve-route-with-request
  (let [request {:method :get :url "http://example.com"}]
    (is (= request (routes/resolve-route routes request)))))

(deftest test-resolve-route-continent
  (let [request (routes/resolve-route routes :continent {:path-params {:id 1}})]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request)))))

(deftest test-resolve-route-continents
  (let [request (routes/resolve-route routes :continents)]
    (is (= :get (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-resolve-route-create-continent
  (let [request (routes/resolve-route routes :create-continent {:edn-body {:id 1 :name "Europe"}})]
    (is (= {:id 1 :name "Europe"} (:edn-body request)))
    (is (= :post (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-resolve-route-delete-continent
  (let [request (routes/resolve-route routes :delete-continent {:path-params {:id 1}})]
    (is (= :delete (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-resolve-route-update-continent
  (let [request (routes/resolve-route routes :update-continent {:edn-body {:id 1 :name "Europe"}})]
    (is (= {:id 1 :name "Europe"} (:edn-body request)))
    (is (= :put (:method request) ))
    (is (= :http (:scheme request) ))
    (is (= "example.com" (:server-name request) ))
    (is (= 80 (:server-port request) ))
    (is (= "/continents/1" (:uri request) ))))

(deftest test-resolve-route-override-defaults
  (let [default {:scheme :https :server-name "other.com" :server-port 8080}
        request (routes/resolve-route routes :continents default)]
    (is (= :get (:method request) ))
    (is (= (:scheme default) (:scheme request) ))
    (is (= (:server-name default) (:server-name request) ))
    (is (= (:server-port default) (:server-port request) ))
    (is (= "/continents" (:uri request) ))))

(deftest test-path-for-routes
  (is (nil? ((routes/path-for-routes routes) nil)))
  (is (nil? ((routes/path-for-routes routes) :not-existing)))
  (are [name opts expected]
    (is (= expected ((routes/path-for-routes routes) name opts)))
    :continents {} "/continents"
    :continents {:query-params {:a 1 :b 2}} "/continents?a=1&b=2"
    :continent {} "/continents/:id"
    :continent {:id 1} "/continents/1"
    :continent {:path-params {:id 1}} "/continents/1"
    :continent {:path-params {:id 1} :query-params {:a 1}} "/continents/1?a=1"
    :create-continent {} "/continents"
    :delete-continent {:id 1} "/continents/1"
    :delete-continent {:path-params {:id 1}} "/continents/1"
    :update-continent {:id 1} "/continents/1"
    :update-continent {:path-params {:id 1}} "/continents/1"))

(deftest test-url-for-routes
  (is (nil? ((routes/url-for-routes routes) nil)))
  (is (nil? ((routes/url-for-routes routes) :not-existing)))
  (are [name opts expected]
    (is (= expected ((routes/url-for-routes routes) name opts)))
    :continents {} "http://example.com/continents"
    :continent {} "http://example.com/continents/:id"
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

#+clj
(deftest test-body
  (with-redefs
    [clj-http/request
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
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (= {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}}
           (http :continents {:query-params {:query "Europe"}}))))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= false (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :post (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (= {:status 201, :body "{:name \"Europe\"}", :headers {"content-type" "application/edn"}}
           (http :create-continent {:as :auto :edn-body {:name "Europe"}})))))

#+clj
(deftest test-http!
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (= {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}}
           (http! :continents {:query-params {:query "Europe"}}))))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :post (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (= {:status 201, :body "{:name \"Europe\"}", :headers {"content-type" "application/edn"}}
           (http! :create-continent {:as :auto :edn-body {:name "Europe"}})))))

(deftest test-http<
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :get (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= {:query "Europe"} (:query-params request)))
       {:status 200 :body [{:id 1 :name "Europe"}] :headers {"Content-Type" "application/edn"}})]
    (is (http< :continents {:query-params {:query "Europe"}})))
  (with-redefs
    [clj-http/request
     (fn [request]
       (is (= true (:throw-exceptions request)))
       (is (= :http (:scheme request)))
       (is (= "example.com" (:server-name request)))
       (is (= 80 (:server-port request)))
       (is (= :post (:method request)))
       (is (= "/continents" (:uri request)))
       (is (= "{:name \"Europe\"}" (:body request)))
       {:status 201 :body (:body request) :headers {"content-type" "application/edn"}})]
    (is (http< :create-continent {:as :auto :edn-body {:name "Europe"}}))))

#+clj
(deftest test-fetch-routes
  (with-redefs [core/client (fn [request]
                              (is (= :get (:method request)))
                              (is (= "http://example.com" (:url request)))
                              {:body (edn/read-string (slurp "test-resources/routes.edn"))})]
    (let [routes (routes/fetch-routes "http://example.com")]
      (is (not (empty? routes)))
      (let [route (:spots routes)]
        (is (= :spots (:route-name route)))
        (is (= :get (:method route)))
        (is (= "/spots" (:path route)))
        (is (= [] (:path-params route)))
        (is (= ["" "spots"] (:path-parts route)))))))

#+clj
(deftest test-read-routes
  (let [routes (routes/read-routes "test-resources/routes.edn")]
    (is (not (empty? routes)))
    (let [route (:spots routes)]
      (is (= :spots (:route-name route)))
      (is (= :get (:method route)))
      (is (= "/spots" (:path route)))
      (is (= [] (:path-params route))))))

#+clj
(deftest test-spit-routes
  (let [old (routes/read-routes "test-resources/routes.edn")
        filename "/tmp/test-spit-routes"]
    (routes/spit-routes filename old)
    (let [new (routes/read-routes filename)]
      (is (= (set (keys old)) (set (keys new))))
      (is (= (map routes/serialize-route (vals old))
             (map routes/serialize-route (vals new)))))))

(deftest test-path-matches
  (let [route (first (routes/path-matches routes "/continents/1"))]
    (is (= "/continents/1" (:uri route)))
    (is (= :continent (:route-name route)))
    (is (= "/continents/:id" (:path route)))
    (is (= {:id "1"} (:path-params route)))))

(comment
  (request :continents)
  (http :continents)
  (http :continent {:path-params {:id -1}})
  (http! :continent {:path-params {:id -1}})
  (http<! :continents)
  (body :continents {:server-name "api.burningswell.com"})
  (body :delete-continent {:server-name "api.burningswell.dev" :path-params {:id 1}})
  (body :delete-continent {:server-name "api.burningswell.dev" :path-params {:id 1}})
  (request :delete-continent {:id 1})
  (<!! (body< :continents {:server-name "api.burningswell.com"}))
  (http< :continentss {:server-name "api.burningswell.com"})
  (<!! (body< :continents {:server-name "api.burningswell.com"}))
  (<!! (http< :continents {:server-name "api.burningswell.dev"})))
