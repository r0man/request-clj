# REQUEST-CLJ [![Build Status](https://travis-ci.org/r0man/request-clj.png)](https://travis-ci.org/r0man/request-clj)

HTTP library for Clojure and ClojureScript.

## Installation

Via Clojars: https://clojars.org/request-clj

## Usage

    (use 'request-clj.core)

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
      :scheme :http
      :server-name "example.com"
      :server-port 80
      :as :auto)

    (request routes :continents)

## License

Copyright © 2013 Roman Scherer

Distributed under the Eclipse Public License, the same as Clojure.
