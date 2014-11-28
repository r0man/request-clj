(defproject request-clj "0.2.10-SNAPSHOT"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "https://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "1.0.1"]
                 [cljs-http "0.1.21"]
                 [routes-clj "0.1.3"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]]
  :aliases {"ci" ["do" ["difftest"] ["lint"]]
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
  :cljsbuild {:test-commands {"node" ["node" :node-runner "target/testable.js"]
                              "phantom" ["phantomjs" :runner "target/testable.js"]
                              "rhino" ["rhino" "-opt" "-1" :rhino-runner "target/testable.js"]}
              :builds [{:source-paths ["target/classes" "target/test-classes"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :advanced
                                   :pretty-print true}}]}
  :deploy-repositories [["releases" :clojars]]
  :prep-tasks [["cljx" "once"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.5"]
                             [com.cemerick/clojurescript.test "0.3.1"]
                             [jonase/eastwood "0.2.0"]
                             [lein-cljsbuild "1.0.3"]
                             [lein-difftest "2.0.0"]
                             [org.clojars.cemerick/cljx "0.5.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]
                   :hooks [leiningen.cljsbuild]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :test-paths ["target/test-classes"]}
             :test {:prep-tasks [["cljsbuild" "once"]]}})
