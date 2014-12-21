(ns todone.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn todone-app [app owner]
  (reify om/IRender
    (render [_]
      (dom/h1 nil (:text app)))))

(om/root todone-app app-state
  {:target (.getElementById js/document "app")})
