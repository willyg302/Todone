(ns todone.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))


;;;; COMPONENTS

; @TODO: SO MANY MAGIC NUMBERS. MUST GET RID OF

(defn header []
  [:header.navbar.navbar-default.navbar-static-top {:role "banner"}
    [:div.container
      [:div.navbar-header
        [:a.navbar-brand {:href "#"}
          [:img {:src "img/todone-logo.svg"}]]]]])

(defn day [w p offset]
  [:rect.day {:width w
              :height w
              :x (* (quot offset 7) (+ w p))
              :y (* (rem offset 7) (+ w p))
              :fill "#eeeeee"
              :data-i offset}])

(defn calendar [days]
  [:div.calendar-bg.container-fluid
    [:div.scroll
      [:svg#calendar {:width (- (* 40 (js/Math.ceil (/ 365 7))) 4)}
        (for [d days]
          ^{:key d} [day 36 4 d])]]])

(defn todone-app []
  [:div
    [header]
    [calendar (range 365)]])


;;;; MAIN

(reagent/render-component [todone-app] (.getElementById js/document "app"))
