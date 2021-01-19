(ns todomvc.init
  (:require
    [rum.core :as rum]
    [todomvc.plumbing :as p]
    [todomvc.todo]
    [goog.events])
  (:import
    [goog.history Html5History EventType]))


(rum/defc root < rum/reactive [app-atom]
  (p/view app-atom
    (rum/react (rum/cursor-in app-atom [:page]))))


;; Super simplistic implementation of url handling - no support for parameters
(defonce history-atom (atom nil))

(defn handle-url-change [app-atom e]
  (when (.-isNavigation ^goog e)
    (.log js/console "Updating bindings")
    (swap! app-atom assoc :page
      (p/resolve-path (.getToken ^Html5History @history-atom)))

    ;; uncomment to scroll to the top to simulate a navigation
    ;; really depends on the app you're going with.
    #_(js/window.scrollTo 0 0)))

(defn setup-history [app-atom]
  (let [history
        (doto (Html5History.)
          (.setPathPrefix
            (str js/window.location.protocol
              "//"
              js/window.location.host))
          (.setUseFragment true)
          (goog.events/listen EventType.NAVIGATE
            ;; wrap in a fn to allow live reloading
            #(handle-url-change app-atom %))

          (.setEnabled true))]
    (reset! history-atom history)))


;; A place for our actual app state - we put it low down here so we're 'forced' to pass it around
;; to the rest of the world..
(defonce app-atom
  (atom
    {}))

(defn refresh []
  (rum/mount (root app-atom) (.getElementById js/document "react")))

(defonce init
  (do
    (reset! app-atom (todomvc.todo/initial-state))
    (setup-history app-atom)
    (p/setup-websocket app-atom)


    true))

(refresh)
