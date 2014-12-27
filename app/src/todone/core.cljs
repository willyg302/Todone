(ns todone.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn todone-app []
  [:h1 (@app-state :text)])

(reagent/render-component [todone-app] (.getElementById js/document "app"))
