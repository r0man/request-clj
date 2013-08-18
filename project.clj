(defproject request-clj "0.1.3-SNAPSHOT"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "http://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "0.7.6"]
                 [cljs-http "0.0.6-SNAPSHOT"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1853"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[com.cemerick/clojurescript.test "0.0.4"]]
                   :plugins [[com.cemerick/austin "0.1.0"]]}}
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
              :test-commands {"unit-tests" ["runners/phantomjs.js" "target/request-test.js"]}}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
