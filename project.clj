(defproject request-clj "0.2.6-SNAPSHOT"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "http://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :clojars}
  :dependencies [[clj-http "0.9.1"]
                 [cljs-http "0.1.11"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2227"]]
  :cljsbuild {:builds []}
  :profiles {:dev {:dependencies [[com.keminglabs/cljx "0.3.2"]]
                   :plugins [[com.keminglabs/cljx "0.3.2"] ;; Must be before Austin: https://github.com/cemerick/austin/issues/37
                             [com.cemerick/austin "0.1.4"]
                             [com.cemerick/clojurescript.test "0.3.1"]
                             [lein-cljsbuild "1.0.3"]]
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
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}}}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
