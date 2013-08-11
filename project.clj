(defproject request-clj "0.0.4"
  :description "A Clojure & ClojureScript library to build url and path fns."
  :url "http://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "0.7.6"]
                 [cljs-http "0.0.4"]
                 [org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[com.cemerick/clojurescript.test "0.0.4"]]}}
  :plugins [[lein-cljsbuild "0.3.2"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:compiler {:output-to "target/request-test.js"
                                   :optimizations :advanced
                                   :pretty-print true}
                        :source-paths ["test"]}
                       {:compiler {:output-to "target/request-debug.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :source-paths ["src"]}
                       {:compiler {:output-to "target/request.js"
                                   :optimizations :advanced
                                   :pretty-print true}
                        :source-paths ["src"]}]
              :crossover-jar true
              :crossover-path ".crossover-cljs"
              :crossovers [request.util]
              :repl-listen-port 9000
              :repl-launch-commands
              {"chromium" ["chromium" "http://localhost:9000/"]
               "firefox" ["firefox" "http://http://localhost:9000/"]}
              :test-commands {"unit-tests" ["runners/phantomjs.js" "target/request-test.js"]}})
