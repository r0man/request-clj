(ns request.core
  (:require [request.platform :as platform]
            [request.util :as util]))

(defmacro defroutes [name routes & {:as opts}]
  `(do (def ~name
         ~(zipmap (map :route-name routes)
                  (map (partial merge opts) routes)))
       (def ~'path-for (request.util/path-for-routes ~name))
       (def ~'url-for (request.util/url-for-routes ~name))
       (defn ~'http [& args#]
         (apply request.platform/http ~name args#))))
