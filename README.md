# REQUEST-CLJ [![Build Status](https://travis-ci.org/r0man/request-clj.png)](https://travis-ci.org/r0man/request-clj)

HTTP library for Clojure and ClojureScript.

## Installation

Via Clojars: https://clojars.org/request-clj

## Usage

    (use 'request-clj.core)

    (defroutes routes
      [{:route-name :continents,
        :method :get,
        :path "/continents",
        :path-params []}
       {:route-name :continent,
        :method :get,
        :path "/continents/:id",
        :path-params [:id]}
       {:route-name :create-continent,
        :method :post,
        :path "/continents",
        :path-params []}
       {:route-name :delete-continent,
        :method :delete,
        :path "/continents/:id",
        :path-params [:id]}
       {:route-name :update-continent,
        :method :put,
        :path "/continents/:id",
        :path-params [:id]}]
      :scheme :http
      :server-name "example.com"
      :server-port 80
      :as :auto)

    (request routes :continents)

## License

Copyright © 2013 Roman Scherer

Distributed under the Eclipse Public License, the same as Clojure.
