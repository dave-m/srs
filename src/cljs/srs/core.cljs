(ns srs.core
  (:require-macros [reagent.ratom :refer [reaction]])
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
            [re-frame.core :refer [register-handler
                        path
                        register-sub
                        dispatch
                        dispatch-sync
                                    subscribe]]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

;; Initial
(def initial-state
  {:regattas {1 {:id 1 :name "Nationals" :details "The Nationals"}
              2 {:id 2 :name "Europeans" :details "The Europeans"
                 :races {1 {:id 1
                            :name "First Race"
                            :laps 3
                            :course "Trapezoid"
                            :status "Not Started"}}}}})

;; Event Handlers
(register-handler
 :initialize
 (fn [db _]
   (merge db initial-state)))

(defn next-id [seq]
  (+ 1 (or (apply max seq) 0)))

(register-handler
 :add-regatta
 (fn [db [_ name]]
   (let [id (next-id (keys (:regattas db)))]
     (assoc-in db [:regattas id] {:id id
                                :name name
                                :details (str "Regatta " name)
                                :races {}}))))

(register-handler
 :add-race
 (fn [db [_ regatta-id race]]
   (let [id (next-id (keys (get-in db [:regattas (int regatta-id) :races])))
         name (:name race)]
     (assoc-in db [:regattas regatta-id :races]
               (merge race {:id id})))))

;; Subscriptions

(register-sub
 :regattas
 (fn [db _]
   (reaction (vals (:regattas @db)))))

(register-sub
 :get-regatta
 (fn [db [_ regatta-id]]
   (reaction (get-in @db [:regattas (int regatta-id)]))))

;; Views

(defn regattas-input []
  (let [val (atom "")
        stop #(do (reset! val ""))
        save #(let [v (-> @val str clojure.string/trim)]
                              (if-not (empty? v)
                                (dispatch [:add-regatta v]))
                              (reset! val ""))]
    (fn [props]
      [:div {:class "mdl-card__actions"}
            [:div {:class "mdl-cell mdl-cell--12-col"}
             [:div {:class "mdl-cell mdl-cell--3-col"}
              [:div {:class "mdl-textfield mdl-js-textfield mdl-textfield--floating-label"}
               [:input {:class "mdl-textfield__input"
                        :type "text"
                        :id "newRegatta"
                        :value @val
                        :on-change #(reset! val (-> % .-target .-value))}]
               [:label {:for "newRegatta" :class "mdl-textfield__label"} "New Regatta"]]]
             [:div {:class "mdl-cell mdl-cell--2-col"}
              [:button {:class "mdl-button mdl-js-button mdl-button--fab mdl-js-ripple-effect mdl-button--colored mdl-shadow--4dp mdl-color--accent"
                        :disabled (if (= 0 (count @val)) "true")
                        :on-click save}
               [:i {:class "material-icons"} "add"]]]]])))

(defn regatta-item [r]
  (fn []
    [:div {:class "section__text mdl-cell mdl-cell--10-col-desktop mdl-cell--6-col-tablet mdl-cell--3-col-phone"}
     [:h5 (:name r)]
     [:p (str (:details r) ". See: ")
      [:a {:href (str "#/races/" (:id r))} "races"]
      (str ", ")
      [:a {:href (str "#/boats/" (:id r))} "boats"]
      (str ".")]])

  )
(defn regattas-card []
  (let [reg (subscribe [:regattas])]
    (fn []
      [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
       [:div {:class "mdk-card mdl-cell mdl-cell--12-col"}
        [:h4 {:class "mdl-cell mdl-cell--12-col"} "Current Regattas"]
        [:div {:class "mdl-card__supporting-text mdl-grid mdl-grid--no-spacing"}
         (for [r @reg]
           ^{:key (:id r)} [regatta-item r])]
        [regattas-input]]])))

(defn race-item [race]
  (fn []
    (println race)
    [:div {:class "section__text mdl-cell mdl-cell--10-col-desktop mdl-cell--6-col-tablet mdl-cell--3-col-phone"}
     [:h5 (:name race)]
     [:ul
      [:li (str "Laps: " (:laps race))]]
      [:li (str "Course: " (:course race))]
      [:li (str "Status: " (:status race))]]))

(defn races-card [regatta-id]
  (let [regatta (subscribe [:get-regatta regatta-id])
        races (vals (:races @regatta))]
    (fn []
        [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
            [:div {:class "mdk-card mdl-cell mdl-cell--12-col"}
             [:h4 {:class "mdl-cell mdl-cell--12-col"}
              "All Races for "
              [:a {:href (str "#/")} (:name @regatta)]
              " Regatta"]
            [:div {:class "mdl-card__supporting-text mdl-grid mdl-grid--no-spacing"}
            (for [r races]
                ^{:key (:id r)} [race-item r])]]])))

(defn page [content & rest]
  [:div {:class "mdl-layout mdl-js-layout mdl-layout--fixed-header"}
   [:header {:class "mdl-layout__header"}
    [:div {:class "mdl-layout__header-row"}
    [:span {:class "mdl-layout-title"} "Sail Regatta Scoring"]
    [:span {:class "mdl-layout-spacer"}]
    [:nav {:class "mdl-navigation"}
    [:a {:class "mdl-navigation__link"
            :href "#/"
            } "Regatta"]
    [:a {:class "mdl-navigation__link"
            :href "#/races"
            } "Races"]
    [:a {:class "mdl-navigation__link" :href "#/races" } "Races"]
    ]]]
    [:main {:class "mdl-layout__content page-content"}
    [:div {:class "mdl-grid"}
    [:div {:class "mdl-cell mdl-cell--12-col mdl-cell--9-col-tablet"} [(apply content rest)]]]]])


(defn home-page []
  [page regattas-card])
(defn races-page [{regatta-id :regatta-id}]
  [page races-card regatta-id])

;; Page routing 
(defn current-page []
  [:div [(session/get :current-page) (session/get :params)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")
(secretary/defroute "/" []
  (session/put! :current-page #'home-page))
(secretary/defroute "/races/:regatta-id" {:as params}
  (session/put! :current-page #'races-page)
  (session/put! :params params))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))


(defonce init (dispatch-sync [:initialize]))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
