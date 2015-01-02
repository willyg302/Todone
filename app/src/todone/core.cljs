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

; Overrides the incorrect versions in cljs-time

(defn within?
  [interval date]
  (or (same-day? (moment/start interval) date)
      (moment/within? interval date)
      (same-day? (moment/end interval) date)))

(defn overlaps?
  [a b]
  (or (within? a (moment/start b))
      (within? b (moment/start a))))

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
  :selected-days (moment/interval (moment/now) (moment/now))
  :days (sorted-map)
  :todos (sorted-map)}))

(def counter (atom 0))

(defn add-todo [text interval]
  (let [id (swap! counter inc)]
    (swap! app-state assoc-in [:todos id] {:id id
                                           :title text
                                           :interval interval})))

(add-todo "Call mom" (moment/interval (moment/date-time 2014 10 9) (moment/date-time 2014 10 9)))
(add-todo "Go to the store" (moment/interval (moment/date-time 2014 11 15) (moment/date-time 2014 12 1)))
(add-todo "This is today" (moment/interval (moment/now) (moment/now)))


(defn add-day [d]
  (swap! app-state assoc-in [:days d] {:id d
                                       :date (moment/plus (moment/start cal) (moment/days d))
                                       :status (rand-int 3)}))

(defn add-days []
  (let [numdays (moment/in-days (moment/interval (moment/start cal) (moment/now)))]
    (doseq [d (range (+ numdays 1))]
      (add-day d))))

(add-days)

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

(defn get-day-color [d m]
  (cond
    (within? (@app-state :selected-days) m) "blue"
    (contains? (@app-state :days) d)
      (case (((@app-state :days) d) :status)
        0 "red"
        1 "green"
        "#eeeeee")
    (moment/after? m (moment/now)) "#888888"
    :else "#eeeeee"))

(defn day [d m w p offset]
  [:rect {:width w
          :height w
          :x (* (quot offset 7) (+ w p))
          :y (* (rem offset 7) (+ w p))
          :fill (get-day-color d m)
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
                        [day d m 36 4 (+ d start-offset)]))]]]))


(defn todo-item [item]
  [:div.panel.panel-default
    [:div.panel-body
      (item :title)]])

(defn todo-list []
  [:div#list.col-md-8
    [:h1.page-header (interval-string (@app-state :selected-days))]
    (for [d (filter #(overlaps? (@app-state :selected-days) (% :interval)) (vals (@app-state :todos)))]
      ^{:key (d :id)} [todo-item d])])


(defn sidebar-panel [title content]
  [:div.panel.panel-default
    [:div.panel-heading title]
    (into [:div.panel-body] content)])


(defn get-longest-chain []
  (let [p (partition-by #(= (% :status) 1) (vals (@app-state :days)))
        c (filter #(= ((first %) :status) 1) p)]
    (first (sort-by count > c))))

(defn get-current-chain []
  (let [p (partition-by #(= (% :status) 1) (vals (@app-state :days)))
        c (filter #(= (% :status) 1) (last p))]
    c))

(defn sidebar []
  [:div#sidebar.col-md-4
    [sidebar-panel "Longest Chain"
      (let [longest-chain (get-longest-chain)
            longest-interval (moment/interval ((first longest-chain) :date)
                                              ((last longest-chain) :date))]
        [
          [:h1.text-center (str (count longest-chain) " days")]
          [:p.text-center (interval-string longest-interval)]])]
    [sidebar-panel "Current Chain" (let [current-chain (get-current-chain)]
        [
          [:h1.text-center (str (count current-chain) " days")]
          [:p.text-center (if (zero? (count current-chain))
            "Better get on that"
            (interval-string (moment/interval ((first current-chain) :date)
                                              ((last current-chain) :date))))]])]])


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
