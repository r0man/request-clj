(defproject request-clj "0.1.12"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "http://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :clojars}
  :dependencies [[clj-http "0.7.8"]
                 [cljs-http "0.1.2"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2080"]]
  :profiles {:dev {:dependencies [[com.keminglabs/cljx "0.3.1"]]
                   :plugins [[com.cemerick/austin "0.1.3"]
                             [com.cemerick/clojurescript.test "0.2.1"]]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}}}
  :plugins [[com.keminglabs/cljx "0.3.1"]
            [lein-cljsbuild "1.0.1"]]
  :hooks [cljx.hooks leiningen.cljsbuild]
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
  :cljsbuild {:test-commands {"phantom" ["phantomjs" :runner "target/testable.js"]}
              :builds [{:source-paths ["target/classes" "target/test-classes"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :advanced
                                   :pretty-print true}}]}
  :test-paths ["target/test-classes"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
