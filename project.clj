(defproject todone "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]]

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
                :preamble ["react/react.min.js"]
                :externs ["react/react.js"]
                :closure-warnings {:externs-validation :off
                                   :non-standard-jsdoc :off}}}]})
