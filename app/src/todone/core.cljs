(ns todone.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :as ajax]
            [cljs-time.core :as t]
            [cljs-time.format :as t-format]))

(enable-console-print!)


;;;; UTILS

(defn same-day?
  [a b]
  (every? true? (map #(= (% a) (% b)) [t/year t/month t/day])))

(defn today?
  [date]
  (same-day? (t/now) date))

(defn date-str
  ([date] (date-str date "MMM d, yyyy"))
  ([date formatter] (t-format/unparse (t-format/formatter formatter) date)))

(defn interval-str
  ([{:keys [start end]}]
    (interval-str start end))
  ([start end]
    (if (same-day? start end)
      (date-str start)
      (str (date-str start) " \u2013 " (date-str end)))))

; Overrides the incorrect version in cljs-time
(defn within?
  [interval date]
  (or (same-day? (t/start interval) date)
      (t/within? interval date)
      (same-day? (t/end interval) date)))

; Overrides the incorrect version in cljs-time
(defn overlaps?
  [a b]
  (or (within? a (t/start b))
      (within? b (t/start a))))


; BEGIN DANGER ZONE
; @TODO: SO MANY MAGIC NUMBERS. MUST GET RID OF
; Also, most of this stuff is temporary until a link to Go is established

;;;; GLOBAL STATE

; @TODO: This is just a placeholder, actually grab from AJAX
(def cal (t/interval (t/date-time 2014 9 9)
                          (t/plus (t/now) (t/years 1))))


(def app-state (atom {
  :selected-days (t/interval (t/now) (t/now))
  :days (sorted-map)
  :todos (sorted-map)}))

(def counter (atom 0))

(defn add-todo [text interval]
  (let [id (swap! counter inc)]
    (swap! app-state assoc-in [:todos id] {:id id
                                           :title text
                                           :interval interval
                                           :completed false})))

(add-todo "Call mom" (t/interval (t/date-time 2014 10 9) (t/date-time 2014 10 9)))
(add-todo "Go to the store" (t/interval (t/date-time 2014 11 15) (t/date-time 2014 12 1)))
(add-todo "This is today" (t/interval (t/now) (t/now)))


(defn add-day [d]
  (swap! app-state assoc-in [:days d] {:id d
                                       :date (t/plus (t/start cal) (t/days d))
                                       :status (rand-int 3)}))

(defn add-days []
  (let [numdays (t/in-days (t/interval (t/start cal) (t/now)))]
    (doseq [d (range (+ numdays 1))]
      (add-day d))))

(add-days)
; END DANGER ZONE


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


;;; DAY

(defn handle-day-click [e m]
  (swap! app-state assoc :selected-days
    (if (.-shiftKey e)
      (let [start (t/start (@app-state :selected-days))]
        (t/interval (t/earliest start m) (t/latest start m)))
      (t/interval m m))))

(defn get-day-color [d m]
  (cond
    (within? (@app-state :selected-days) m) "blue"
    (contains? (@app-state :days) d)
      (case (((@app-state :days) d) :status)
        0 "red"
        1 "green"
        "#eeeeee")
    (t/after? m (t/now)) "#888888"
    :else "#eeeeee"))

(defn day [d m w p offset]
  [:rect {:width w
          :height w
          :x (* (quot offset 7) (+ w p))
          :y (* (rem offset 7) (+ w p))
          :fill (get-day-color d m)
          :data-i m
          :on-click #(handle-day-click % m)}])


(defn calendar [num-days]
  (let [offset (-> cal t/start t/day-of-week (mod 7))
        cal-width (-> offset (+ num-days) (/ 7) js/Math.ceil (* 40) (- 4))]
    [:div.calendar-bg.container-fluid
      [:div.scroll
        [:svg#calendar {:width cal-width}
          (for [d (range num-days)]
            ^{:key d} (let [m (t/plus (t/start cal) (t/days d))]
                        [day d m 36 4 (+ d offset)]))]]]))


;;; TODO LIST

(defn todo-item [item]
  [:div.panel.panel-default
    [:div.panel-body
      (item :title)]])

(defn todo-list []
  [:div#list.col-md-8
    [:h1.page-header (interval-str (@app-state :selected-days))]
    (for [d (filter #(overlaps? (@app-state :selected-days) (% :interval))
                    (vals (@app-state :todos)))]
      ^{:key (d :id)} [todo-item d])])


;;; SIDEBAR

(defn sidebar-panel [title content]
  [:div.panel.panel-default
    [:div.panel-heading title]
    (into [:div.panel-body] content)])

(defn chain-info-box [title chain]
  (let [c (count chain)]
    [sidebar-panel title
      [
        [:h1.text-center (str c " day" (if-not (= c 1) "s"))]
        (when (pos? c)
          [:p.text-center (interval-str ((first chain) :date)
                                        ((last chain) :date))])]]))

(defn sidebar []
  (let [p (partition-by #(= (% :status) 1) (vals (@app-state :days)))]
    [:div#sidebar.col-md-4
      (chain-info-box "Longest Chain"
                      (->> p
                           (filter #(= ((first %) :status) 1))
                           (sort-by count)
                           last))
      (chain-info-box "Current Chain"
                      (filter #(= (% :status) 1) (last p)))]))


;;;; MAIN

(defn body []
  [:div#body.container
    [:div.row
      [todo-list]
      [sidebar]]])

(defn todone-app []
  [:div
    [header]
    [calendar (t/in-days cal)]
    [body]])

(reagent/render-component [todone-app] (.getElementById js/document "app"))
