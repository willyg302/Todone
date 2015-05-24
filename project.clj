(defproject todone "0.1.0-SNAPSHOT"
  :description "Go from to-do to done without breaking the chain"
  :url "https://github.com/willyg302/Todone"

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3291"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0"]
                 [cljs-ajax "0.3.11"]
                 [com.andrewmcveigh/cljs-time "0.3.5"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

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
                :closure-warnings {:externs-validation :off
                                   :non-standard-jsdoc :off}}}]})
