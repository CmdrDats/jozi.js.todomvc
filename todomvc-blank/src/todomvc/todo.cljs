(ns todomvc.todo
  (:require
    [rum.core :as rum]
    [todomvc.plumbing :as p]
    [clojure.string :as str]
    [inflections.core :as inflections]))

(defn initial-state []
  {:page :todo})

(rum/defc index < rum/reactive [app-atom]
  [:div "Hello"])

(defmethod p/view :todo [app-atom _]
  (index app-atom))

(p/routes!
  {"/" :todo})
