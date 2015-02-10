(defproject request-clj "0.2.15-SNAPSHOT"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "https://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "1.0.1"]
                 [cljs-http "0.1.25"]
                 [routes-clj "0.1.7"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2816" :scope "provided"]]
  :aliases {"ci" ["do" ["cleantest"] ["lint"]]
            "cleantest" ["do" "clean," "cljx" "once," "test," "cljsbuild" "test"]
            "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]
            "lint" ["do"  ["eastwood"]]
            "test-ancient" ["test"]}
  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}
  :cljsbuild {:builds [{:id "test"
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :advanced
                                   :pretty-print true}
                        :notify-command ["phantomjs" :cljs.test/runner "target/testable.js"]
                        :source-paths ["target/classes" "target/test-classes"]}]
              :test-commands {"node" ["node" :node-runner "target/testable.js"]
                              "phantom" ["phantomjs" :runner "target/testable.js"]}}
  :deploy-repositories [["releases" :clojars]]
  :prep-tasks [["cljx" "once"]]
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.1.5"]]
                   :plugins [[com.cemerick/clojurescript.test "0.3.3"]
                             [com.keminglabs/cljx "0.5.0"]
                             [jonase/eastwood "0.2.1"]
                             [lein-cljsbuild "1.0.4"]
                             [lein-difftest "2.0.0"]]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :test-paths ["target/test-classes"]}})
