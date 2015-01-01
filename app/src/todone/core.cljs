(ns todone.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ajax]
            [cljs-time.core :as moment]))

(enable-console-print!)


;;;; UTILS

; @TODO: This is just a placeholder, actually grab from AJAX
(def cal (moment/interval (moment/date-time 2014 9 9)
                          (moment/plus (moment/now) (moment/years 1))))

(defn same-day?
  [a b]
  (and (= (moment/year a) (moment/year b))
       (= (moment/month a) (moment/month b))
       (= (moment/day a) (moment/day b))))

(defn today?
  [day]
  (same-day? (moment/now) day))


; @TODO: SO MANY MAGIC NUMBERS. MUST GET RID OF

;;;; GLOBAL STATE

(def app-state (atom {:selected-day (moment/now)}))


;;;; COMPONENTS

(defn header []
  [:header
    [:div.container
      [:a.brand {:href "#"}
        [:img {:src "img/todone-logo.svg"}]]
      [:button {:class "btn btn-default navbar-btn pull-right"
                :type "button"
                :on-click #(ajax/POST
                            "/logout"
                            {:handler (fn [response] (js/location.reload))})}
               "Log Out"]]])

(defn day [m w p offset]
  [:rect {:width w
          :height w
          :x (* (quot offset 7) (+ w p))
          :y (* (rem offset 7) (+ w p))
          :fill (if (same-day? (@app-state :selected-day) m) "red" "#eeeeee")
          :data-i m
          :on-click #(swap! app-state assoc :selected-day m)}])

(defn calendar [num-days]
  (let [start-offset (mod (moment/day-of-week (moment/start cal)) 7)
        cal-width (- (* 40 (js/Math.ceil (/ (+ num-days start-offset) 7))) 4)]
    [:div.calendar-bg.container-fluid
      [:div.scroll
        [:svg#calendar {:width cal-width}
          (for [d (range num-days)]
            ^{:key d} (let [m (moment/plus (moment/start cal) (moment/days d))]
                        [day m 36 4 (+ d start-offset)]))]]]))

(defn todone-app []
  [:div
    [header]
    [calendar (moment/in-days cal)]])


;;;; MAIN

(reagent/render-component [todone-app] (.getElementById js/document "app"))
