(ns todone.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ajax]
            [cljs-time.core :as moment]
            [cljs-time.format :as time-format]))

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

(defn within?
  [interval date]
  (or (same-day? (moment/start interval) date)
      (moment/within? interval date)
      (same-day? (moment/end interval) date)))

(defn interval-string
  [interval]
  (let [formatter (time-format/formatter "MMM d, yyyy")
        format-day (fn [day] (time-format/unparse formatter day))
        start (moment/start interval)
        end (moment/end interval)]
    (if (same-day? start end)
      (format-day start)
      (str (format-day start) " \u2013 " (format-day end)))))


; @TODO: SO MANY MAGIC NUMBERS. MUST GET RID OF

;;;; GLOBAL STATE

(def app-state (atom {
  :selected-days (moment/interval (moment/now) (moment/now))}))


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

(defn handle-day-click [e m]
  (if (.-shiftKey e)
    (let [start (moment/start (@app-state :selected-days))]
      (swap! app-state assoc :selected-days
        (moment/interval (moment/earliest start m)
                         (moment/latest start m))))
    (swap! app-state assoc :selected-days (moment/interval m m))))

(defn get-day-color [m]
  (cond
    (within? (@app-state :selected-days) m) "red"
    (moment/after? m (moment/now)) "#888888"
    :else "#eeeeee"))

(defn day [m w p offset]
  [:rect {:width w
          :height w
          :x (* (quot offset 7) (+ w p))
          :y (* (rem offset 7) (+ w p))
          :fill (get-day-color m)
          :data-i m
          :on-click (fn [e] (handle-day-click e m))}])

(defn calendar [num-days]
  (let [start-offset (mod (moment/day-of-week (moment/start cal)) 7)
        cal-width (- (* 40 (js/Math.ceil (/ (+ num-days start-offset) 7))) 4)]
    [:div.calendar-bg.container-fluid
      [:div.scroll
        [:svg#calendar {:width cal-width}
          (for [d (range num-days)]
            ^{:key d} (let [m (moment/plus (moment/start cal) (moment/days d))]
                        [day m 36 4 (+ d start-offset)]))]]]))



(defn todo-list []
  [:div#list.col-md-8
    [:h1.page-header (interval-string (@app-state :selected-days))]
    ]
  )


; <div id="list" class="col-md-8">
;   <h1 class="page-header">Oct 9, 2014 &ndash; Nov 11, 2014</h1>
;   <div class="panel panel-default">
;     <div class="panel-body">
;       Walk the dog
;     </div>
;   </div>
;   <div class="panel panel-default">
;     <div class="panel-body">
;       Call Mom
;     </div>
;   </div>
; </div>


(defn sidebar-panel [title content]
  [:div.panel.panel-default
    [:div.panel-heading title]
    [:div.panel-body content]])

(defn sidebar []
  [:div#sidebar.col-md-4
    [sidebar-panel "Longest Chain" "yo"]
    [sidebar-panel "Current Chain" "yo"]])



; <div id="sidebar" class="col-md-4">
;   <div class="panel panel-default">
;     <div class="panel-heading">Longest Chain</div>
;     <div class="panel-body">
;       <h1 class="text-center"><span id="longest-length"></span> days</h1>
;       <p class="text-center"><small id="longest-range"></small></p>
;     </div>
;   </div>
;   <div class="panel panel-default">
;     <div class="panel-heading">Current Chain</div>
;     <div class="panel-body">
;       <h1 class="text-center"><span id="current-length"></span> days</h1>
;       <p class="text-center"><small id="current-range"></small></p>
;     </div>
;   </div>
; </div>


(defn body []
  [:div#body.container
    [:div.row
      [todo-list]
      [sidebar]]])

(defn todone-app []
  [:div
    [header]
    [calendar (moment/in-days cal)]
    [body]])



;;;; MAIN

(reagent/render-component [todone-app] (.getElementById js/document "app"))
