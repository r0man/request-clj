(defproject request-clj "0.3.1"
  :description "A HTTP library for Clojure & ClojureScript."
  :url "https://github.com/r0man/request-clj"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "3.4.1"]
                 [cheshire "5.6.3"]
                 [cljs-http "0.1.42"]
                 [com.cognitect/transit-clj "0.8.293"]
                 [http-kit "2.2.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [routes-clj "0.1.10"]]
  :aliases
  {"ci" ["do"
         ["clean"]
         ["difftest"]
         ["doo" "node" "node" "once"]
         ["doo" "phantom" "none" "once"]
         ["doo" "phantom" "advanced" "once"]
         ["lint"]]
   "lint" ["do"  ["eastwood"]]}
  :cljsbuild
  {:builds
   [{:id "none"
     :compiler
     {:main 'request.test.runner
      :optimizations :none
      :output-dir "target/none"
      :output-to "target/none.js"
      :parallel-build true
      :pretty-print true
      :verbose false}
     :source-paths ["src" "test"]}
    {:id "node"
     :compiler
     {:main 'request.test.runner
      :optimizations :none
      :output-dir "target/node"
      :output-to "target/node.js"
      :parallel-build true
      :pretty-print true
      :target :nodejs
      :verbose false}
     :source-paths ["src" "test"]}
    {:id "advanced"
     :compiler
     {:main 'request.test.runner
      :optimizations :advanced
      :output-dir "target/advanced"
      :output-to "target/advanced.js"
      :parallel-build true
      :pretty-print true
      :verbose false}
     :source-paths ["src" "test"]}]}
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:plugins [[jonase/eastwood "0.2.3"]
                             [lein-cljsbuild "1.1.4"]
                             [lein-doo "0.1.7"]
                             [lein-difftest "2.0.0"]]
                   :test-paths ["target/test-classes"]}
             :repl {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                   [reloaded.repl "0.2.3"]]
                    :init-ns user
                    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}})
