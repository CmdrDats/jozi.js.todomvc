(ns todomvc.plumbing
  (:require
    [taoensso.sente :as sente :refer (cb-success?)]
    [clojure.core.async :as async]
    [rum.core :as rum]))

(defonce comms-atom (atom {}))

(defn ?csrf-token []
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(defn send-message [id data]
  ((:send-fn @comms-atom) [id data]))

(defmulti receive (fn [app-atom data & [type socket]] type))

(defmethod receive :default [app-atom data type]
  (println "Unhandled comms message: " type))

(defn message-handler [state-atom {:as socket :keys [id ?data event]}]
  (let [[id ?data] (if (= id :chsk/recv) ?data [id ?data])]
    ;; We don't really care for these low-key, but periodic checks

    (when-not (#{:chsk/ws-ping :chsk/state} id)
      (println "Comms Received:" id)
      (receive state-atom ?data id socket))
    nil))

;; Simple setup - doesn't handle connection issues or dropped messages/responses
;; but it'll do for this.
(defn setup-websocket [app-atom]
  ;; {:keys [chsk ch-recv send-fn state]}
  (reset! comms-atom
    (sente/make-channel-socket-client!
      "/chsk"                                               ; Note the same path as before
      (?csrf-token)

      {:type :auto                                          ; e/o #{:auto :ajax :ws}
       }))

  (sente/start-chsk-router!
    (:ch-recv @comms-atom)
    (partial #'message-handler app-atom)))



;; View Dispatch work - A very simplistic dispatch method, but it does the job!
(defonce view-dispatch (atom {}))

(defn routes! [pathkey-map]
  (swap! view-dispatch merge pathkey-map))

(defn resolve-path [path]
  (get @view-dispatch path :not-found))

(defmulti view (fn [app-atom screen] screen))

(defmethod view :default [app-atom screen]
  [:h1 "No screen defined for " (str screen)])


;; Action dispatch micro-framework...
(defmulti dispatch (fn [app-atom action & params] action))

(defmethod dispatch :default [app-atom action params]
  (println "Unhandled action: " action " -  " (pr-str params)))


;; Helper functions
(defn input-props [val-atom]
  {:value (or (rum/react val-atom) "")
   :on-change #(reset! val-atom (.. % -target -value))})

(defn checkbox-props [val-atom change-fn]
  {:type "checkbox"
   :checked (if (rum/react val-atom) true false)
   :on-change
   (fn []
     (let [new (not @val-atom)]
       (reset! val-atom new)
       (change-fn new)))})