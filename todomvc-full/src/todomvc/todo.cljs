(ns todomvc.todo
  (:require
    [rum.core :as rum]
    [todomvc.plumbing :as p]
    [clojure.string :as str]
    [inflections.core :as inflections]))

(defn initial-state []
  {:page :todo
   :new-item
   {:uid (random-uuid)
    :title ""
    :completed false}

   :items
   (->>
     [{:uid (random-uuid)
       :completed true
       :dateadded (.now js/Date)
       :title "Taste JavaScript"}

      {:uid (random-uuid)
       :completed false
       :dateadded (.now js/Date)
       :title "Buy a unicorn"}]

     (map (juxt :uid identity))
     (into {}))})

(defmethod p/receive :chsk/handshake [app-atom _]
  (p/send-message :todo/start {}))

(defmethod p/receive :todo/start [app-atom data]
  (swap! app-atom assoc :items (:items data)))

(defmethod p/receive :todo/update [app-atom data]
  (swap! app-atom assoc-in [:items (:uid (:item data))] (:item data)))

(defmethod p/receive :todo/delete [app-atom data]
  (swap! app-atom update :items
    #(apply dissoc % (:uids data))))

(defmethod p/receive :todo/complete [app-atom data]
  (swap! app-atom update :items
    (fn [items]
      (reduce (fn [is i] (assoc-in is [i :completed] true)) items (:uids data)))))


(defmethod p/dispatch :todo/new [app-atom _]
  (let [item
        (->
          (:new-item @app-atom)
          (update :title str/trim)
          (assoc :dateadded (.now js/Date)))]
    (swap! app-atom
      (fn [a]
        (->
          a
          (update :items assoc (:uid item) item)
          (assoc :new-item {:title "" :uid (random-uuid)}))))
    (p/send-message :todo/save {:item item})))

(defmethod p/dispatch :todo/edit [app-atom _ params]
  (swap! (:item-atom params) dissoc :editing)
  (p/send-message :todo/save {:item @(:item-atom params)}))

(defmethod p/dispatch :todo/clear-completed [app-atom _ _]
  (let [completed-uids
        (->>
          (:items @app-atom)
          (vals)
          (filter :completed)
          (map :uid))]
    (swap! app-atom update :items
      #(apply dissoc % completed-uids))
    (p/send-message :todo/delete {:uids completed-uids})))

(defmethod p/dispatch :todo/delete [app-atom _ params]
  (swap! app-atom update :items dissoc (get-in params [:item :uid]))
  (p/send-message :todo/delete {:uids [(get-in params [:item :uid])]}))

(defmethod p/dispatch :todo/complete-all [app-atom _ _]
  (let [complete-uids
        (->>
          (:items @app-atom)
          (vals)
          (remove :completed)
          (map :uid))]

    (swap! app-atom update :items
      (fn [items]
        (reduce (fn [is i] (assoc-in is [i :completed] true)) items complete-uids)))
    (p/send-message :todo/complete {:uids complete-uids})))

(rum/defc todo-new < rum/reactive [app-atom]
  [:div
   [:input.new-todo
    (merge
      (p/input-props (rum/cursor-in app-atom [:new-item :title]))
      {:placeholder "What needs to be done?"
       :autoFocus true
       :on-key-up
       #(when (= 13 (.-keyCode %))
          (p/dispatch app-atom :todo/new {})
          )})]])

(rum/defc todo-line < rum/reactive [app-atom item-atom item]
  [:li
   {:class
    [(if (:completed item) "completed")
     (if (:editing item) "editing")]}

   (cond
     (:editing item)
     [:input.edit
      (merge
        (p/input-props (rum/cursor-in item-atom [:title]))
        {:autoFocus true
         :on-blur #(p/dispatch app-atom :todo/edit {:item-atom item-atom})
         :on-key-down
         #(when (= 13 (.-keyCode %))
            (p/dispatch app-atom :todo/edit {:item-atom item-atom}))})]

     :else
     [:div.view
      {:on-double-click #(swap! item-atom assoc :editing true)}
      [:input.toggle
       (p/checkbox-props (rum/cursor-in item-atom [:completed])
         #(p/dispatch app-atom :todo/edit {:item-atom item-atom}))]
      [:label (:title item)]
      [:button.destroy
       {:on-click #(p/dispatch app-atom :todo/delete {:item item})}]])])

(rum/defc todo-list < rum/reactive [app-atom items-atom]
  (let [all-complete? (empty? (remove :completed (vals (rum/react items-atom))))]
    [:section.main
     [:input#toggle-all.toggle-all
      {:type "checkbox"
       :on-change #(p/dispatch app-atom :todo/complete-all {})
       :checked all-complete?}]
     [:label {:for "toggle-all"} "Mark all as complete"]
     (->>
       (rum/react items-atom)
       (vals)
       (sort-by :dateadded)
       (map #(todo-line app-atom (rum/cursor-in app-atom [:items (:uid %)]) %))
       (into [:ul.todo-list]))]))

(defn view-items [app-atom stage]
  (rum/derived-atom [(rum/cursor-in app-atom [:items])] :view
    (fn [items]
      (->>
        items
        (filter
          (fn [[k v]]
            (case stage
              :active (not (:completed v))
              :completed (:completed v)
              :all true)))
        (into {})))))

(rum/defc todo-count < rum/reactive [app-atom]
  (let [pending (rum/react (view-items app-atom :active))]
    [:span.todo-count [:strong (inflections/pluralize (count pending) "item")] " left"]))


(rum/defc todo-footer < rum/reactive [app-atom stage]
  [:footer.footer
   (todo-count app-atom)

   [:ul.filters
    [:li
     [:a {:href "#/" :class [(if (= stage :all) "selected")]} "All"]]
    [:li
     [:a {:href "#/active" :class [(if (= stage :active) "selected")]} "Active"]]
    [:li
     [:a {:href "#/completed" :class [(if (= stage :completed) "selected")]} "Completed"]]]

   (when-not (empty? (rum/react (view-items app-atom :completed)))
     [:button.clear-completed
      {:on-click #(p/dispatch app-atom :todo/clear-completed)}
      "Clear completed"])])

(rum/defc index < rum/reactive [app-atom stage]
  (let [items-atom (view-items app-atom stage)
        tasks-count-atom (rum/react (rum/derived-atom [items-atom] ::count count))]
    [[:section.todoapp
      [:header.header
       [:h1 "TodoMVC"]
       (todo-new app-atom)]

      (when (pos? tasks-count-atom)
        (todo-list app-atom items-atom))

      (when (pos? tasks-count-atom)
        (todo-footer app-atom stage))]

     [:footer.info
      [:p "Double-click to edit a todo"]
      [:p "Template by" [:a {:href "http://sindresorhus.com"} "Sindre Sorhus"]]
      [:p "Created by" [:a {:href "http://todomvc.com"} "you"]]
      [:p "Part of" [:a {:href "http://todomvc.com"} "TodoMVC"]]]]))

(defmethod p/view :todo [app-atom _]
  (index app-atom :all))

(defmethod p/view :todo/active [app-atom _]
  (index app-atom :active))

(defmethod p/view :todo/completed [app-atom _]
  (index app-atom :completed))

(p/routes!
  {"/" :todo
   "/active" :todo/active
   "/completed" :todo/completed})
