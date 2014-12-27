(defproject todone "0.1.0-SNAPSHOT"
  :description "Go from to-do to done without breaking the chain"
  :url "https://github.com/willyg302/Todone"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0-alpha"]
                 [cljs-ajax "0.3.3"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "todone"
              :source-paths ["app/src"]
              :compiler {
                :output-to "dist/js/main.js"
                :optimizations :advanced
                :elide-asserts true
                :pretty-print false
                :output-wrapper false
                :preamble ["reagent/react.min.js"]
                :externs ["reagent/react.js"]
                :closure-warnings {:externs-validation :off
                                   :non-standard-jsdoc :off}}}]})
