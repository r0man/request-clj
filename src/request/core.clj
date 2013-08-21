(ns request.core
  (:require [request.platform :as platform]
            [request.util :as util]))

(defmacro defroutes [name routes & [opts]]
  (let [routes# routes
        opts# (eval opts)]
    `(do (def ~name
           ~(zipmap (map :route-name routes#)
                    (map (partial merge opts#) routes#)))
         (def ~'path-for (request.util/path-for-routes ~name))
         (def ~'url-for (request.util/url-for-routes ~name))
         (defn ~'body [& args#]
           (apply request.platform/body ~name args#))
         (defn ~'body<! [& args#]
           (apply request.platform/body<! ~name args#))
         (defn ~'http [& args#]
           (apply request.platform/http ~name args#))
         (defn ~'http<! [& args#]
           (apply request.platform/http<! ~name args#)))))
