(ns todomvc.core
  (:require
    [ring.middleware.keyword-params]
    [ring.middleware.params]
    [ring.middleware.anti-forgery]
    [ring.middleware.session]
    [ring.middleware.reload]
    [ring.middleware.resource]
    [org.httpkit.server]
    [rum.core :as rum]
    [compojure.core :as c]
    [compojure.route :as route]
    [todomvc.comms :as comms]
    [clojure.tools.reader.edn :as edn])
  (:gen-class))








(rum/defc base-template [body]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title "Template • TodoMVC"]
    [:link {:rel "stylesheet" :href "css/base.css"}]
    [:link {:rel "stylesheet" :href "css/index.css"}]]
   body])

(rum/defc index [{:keys [csrf-token]}]
  (base-template
    [:body
     [:div#sente-csrf-token {:data-csrf-token csrf-token}]
     [:div#react "Loading..."]
     [:script {:src "apps/cljs-base.js"}]
     [:script {:src "apps/dev.js"}]]))

(defonce app-atom (atom {:items {}}))

(defmethod comms/receive :todo/start [req _ _]
  {:items (:items @app-atom)})

(defmethod comms/receive :todo/save [req _ {:keys [item] :as data}]
  (swap! app-atom update :items assoc (:uid item) item)
  (comms/broadcast :todo/update {:item item})
  nil)

(defmethod comms/receive :todo/delete [req _ {:keys [uids]}]
  (swap! app-atom update :items #(apply dissoc % uids))
  (comms/broadcast :todo/delete {:uids uids})
  nil)

(defmethod comms/receive :todo/complete [req _ {:keys [uids]}]
  (swap! app-atom update :items
    (fn [items]
      (reduce (fn [is i] (assoc-in is [i :completed] true)) items uids)))
  (comms/broadcast :todo/complete {:uids uids})
  nil)





(defn root [req]
  (let [csrf-token (force ring.middleware.anti-forgery/*anti-forgery-token*)]
    {:status 200
     :body (rum/render-html (index {:csrf-token csrf-token}))
     :headers {"Content Type" "text/html"}}))



(c/defroutes app
  (c/GET "/" [] #'root)
  (c/GET "/chsk" [] comms/ring-ajax-get-or-ws-handshake)
  (c/POST "/chsk" [] comms/ring-ajax-post)
  (route/not-found #'root))

(def app-handler
  (->
    #'app
    ring.middleware.keyword-params/wrap-keyword-params
    ring.middleware.params/wrap-params
    ring.middleware.anti-forgery/wrap-anti-forgery
    ring.middleware.session/wrap-session

    (ring.middleware.reload/wrap-reload)
    (ring.middleware.resource/wrap-resource "public")))

(defn -main [& args]
  (println "starting")
  (try
    (reset! app-atom (edn/read-string (slurp "state.edn")))
    (catch Exception e
      ;; It's ok, you did your best, buddy.
      ))

  (add-watch app-atom :write
    (fn [k r o n]
      (spit "state.edn" (pr-str n))))

  (comms/start-router!)
  (org.httpkit.server/run-server #'app-handler {:port 8040})

  )