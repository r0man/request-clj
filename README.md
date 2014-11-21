# REQUEST-CLJ
  [![Build Status](https://travis-ci.org/r0man/request-clj.png)](https://travis-ci.org/r0man/request-clj)
  [![Dependencies Status](http://jarkeeper.com/r0man/request-clj/status.png)](http://jarkeeper.com/r0man/request-clj)

HTTP and routing library for Clojure and ClojureScript.

## Installation

[![Current Version](https://clojars.org/request-clj/latest-version.svg)](https://clojars.org/request-clj)

## Usage

```
(require '[request.routes :as http :refer [defroutes]])

(defroutes my-routes
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
    :path-params [:id]}])

(def client
  (new-client
   {:scheme :http
    :server-name "example.com"
    :server-port 80}))

(http/get client :continents {:query-params {:query "Europe"}})
;=> {:status 200 :body ... :headers}
```

## License

Copyright © 2014 r0man

Distributed under the Eclipse Public License, the same as Clojure.
