(ns todomvc.comms
  (:require
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(defn user-uuid [u]
  (:client-id u))


(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {:user-id-fn user-uuid})]

  (defonce ring-ajax-post                ajax-post-fn)
  (defonce ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (defonce ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (defonce chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (defonce connected-uids                connected-uids) ; Watchable, read-only atom
  )


(defmulti receive (fn [req id data] id))
(defmethod receive :default [req id data]
  (println "Received: " id " - unhandled.")
  nil)

(def ignored-ids #{:chsk/ws-ping})

(defn comms-receive* [{:keys [?data send-fn client-id id] :as req}]
  (println "Receive start")
  (try
    (when-not (ignored-ids id)
      (let [response (receive req id ?data)]
        (println "response: " (pr-str response))
        (when response
          (println "fdsafds " client-id)
          (chsk-send! client-id [id response]))))
    (catch Throwable e
      (.printStackTrace e)
      ;; Just don't trip over yourself, mkay?
      ))
  (println "Receive end")
  nil)

(defn broadcast [id data]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [id data])))

(defn comms [req]
  (comms-receive* req))

(defn start-router! []
  (sente/start-chsk-router! ch-chsk comms)
  )