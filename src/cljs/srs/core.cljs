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
                            :status "Not Started"
                            :boats {1 {:id 1
                                       :name "David"
                                       :type "Laser Full"}}}}}}})

;; Event Handlers
(register-handler
 :initialize (fn [db _]
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
 (fn [db [_ {regatta-id :regatta-id
             :as input-race} race]]
   (let [id (next-id (keys (get-in db [:regattas (int regatta-id) :races])))
         race (dissoc input-race :regatta-id)]
     (assoc-in db [:regattas (int regatta-id) :races id]
               (merge race {:id id :status "Not Started"})))))

;; Subscriptions

(register-sub
 :regattas
 (fn [db _]
   (reaction (vals (:regattas @db)))))

(register-sub
 :get-regatta
 (fn [db [_ regatta-id]]
   (reaction (get-in @db [:regattas (int regatta-id)]))))

(register-sub
 :get-race
 (fn [db [_ regatta-id race-id]]
   (reaction (get-in @db [:regattas (int regatta-id) :races (int race-id)]))))
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
      [:a {:href (str "#/regattas/" (:id r) "/races")} "races"]
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

(defn add-race [{:keys [regatta-id on-stop]}]
  (let [name (atom "")
        course (atom "")

        laps (atom "")]
    (fn []
      [:div {:class "mdl-cell mdl-cell-6"}
       [:button {:class "mdl-button mdl-js-button mdl-button-mini-fab"
                 :on-click on-stop
                 :style {:float "right"}}
        [:id {:class "material-icons"} "cancel"]]
       [:div {:class "mdl-grid"}
        [:div {:class "mdl-cell--10-col mdl-textfield mdl-js-textfield"}
         [:label {:class "mdl-textfield__label" :for "addRaceName"} "Race Name"]
         [:input {:class "mdl-textfield__input"
                  :id "addRaceName"
                        :type "text"
                        :value @name
                   :on-change #(reset! name (-> % .-target .-value))}]]
        [:div {:class "mdl-cell--10-col mdl-textfield mdl-js-textfield"}
         [:label {:class "mdl-textfield__label" :for "addRaceCourse"} "Course"]
         [:input {:class "mdl-textfield__input"
                  :id "addRaceCourse"
                        :type "text"
                        :value @course
                  :on-change #(reset! course (-> % .-target .-value))}]]
        [:div {:class "mdl-cell--10-col mdl-textfield mdl-js-textfield"}
         [:label {:class "mdl-textfield__label" :for "addRaceLaps"} "Laps"]
         [:input {:class "mdl-textfield__input"
                  :id "addRaceLaps"
                        :type "number"
                        :value @laps
                  :on-change #(reset! laps (-> % .-target .-value))}]]
        [:button {:class "mdl-cell--2-col mdl-button mdl-js-button mdl-button--raised mdl-button--accent"
                  :on-click #(dispatch [:add-race {:regatta-id (int regatta-id)
                                                    :name @name
                                                    :course @course
                                                    :laps @laps}])}
          [:span "Add"]]
        ]])))

(defn race-item [race]
  (fn []
    [:div {:class "race-item mdl-card mdl-cell--4-col mdl-shadow--2dp"}
     [:div {:class "mdl-card__title mdl-card--border"}
      [:h5 {:class "mdl-card__titile-text"} (:name race)]]
     [:div {:class "mdl-card__supporting-text"}
      [:ul
       [:li (str "Laps: " (:laps race))]
       [:li (str "Course: " (:course race))]
       [:li (str "Status: " (:status race))]]]
     [:div {:class "mdl-card__actions mdl-card--border"}
      [:button {:class "races-boats mdl-button mdl-js-button mdl-button--raised mdl-button--colored"
                :style {:float "right"}}
       [:a {:href (str "#/regattas/" (:regatta-id race) "/races/" (:id race) "/boats")}
        "Boats"]]]]))

(defn races-card [regatta-id]
  (let [regatta (subscribe [:get-regatta regatta-id])
        races (vals (:races @regatta))
        adding (atom false)]
    (fn []
      [:section {:class "section--center mdl-shadow--2dp"}
       [:div {:class "mdl-grid"}
             [:h4 {:class "mdl-cell mdl-cell--10-col"}
              "All Races for " (:name @regatta) " Regatta"]
        (when-not @adding [:button {:class "races-add mdl-cell--2-col mdl-button mdl-js-button mdl-button--raised mdl-button--accent"
                   :on-click #(reset! adding true)}
          [:span "Add Race"]])
        (when @adding
          [add-race {:regatta-id (:id @regatta)
                     :on-stop #(reset! adding false)}]) 
        ]
       (when-not @adding
         [:div {:class "mdl-grid"}
          (for [r races] ^{:key (:id r)} [race-item (merge r {:regatta-id (:id @regatta)})])])])))

(defn boat-item [boat]
  (fn []
    [:div {:class "boat-item mdl-card mdl-cell--4-col mdl-shadow--2dp"}
     [:div {:class "mdl-card__title mdl-card--border"}
      [:h5 {:class "mdl-card__titile-text"} (:name boat)]]
     [:div {:class "mdl-card__supporting-text"}
      [:ul
       [:li (str "Name: " (:name boat))]
       [:li (str "Type: " (:type boat))]]]
     [:div {:class "mdl-card__actions"}]]))

(defn boats-card [regatta-id race-id]
  (let [race (subscribe [:get-race regatta-id race-id])
        boats (vals (:boats @race))
        adding (atom false)]
    (fn []
      [:section {:class "section--center mdl-shadow--2dp"}
       [:div {:class "mdl-grid"}
             [:h4 {:class "mdl-cell mdl-cell--10-col"}
              "All Boats for the " (:name @race) " race"]
        (when-not @adding [:button {:class "boats-add mdl-cell--2-col mdl-button mdl-js-button mdl-button--raised mdl-button--accent"
                   :on-click #(reset! adding true)}
          [:span "Add Boat"]])
        ]
       (when-not @adding
         [:div {:class "mdl-grid"}
          (for [b boats] ^{:key (:id b)} [boat-item b])])])))

(defn page [content & rest]
  [:div {:class "mdl-layout mdl-js-layout mdl-layout--fixed-header"}
   [:header {:class "mdl-layout__header"}
    [:div {:class "mdl-layout__header-row"}
    [:span {:class "mdl-layout-title"} "Sail Regatta Scoring"]
    [:span {:class "mdl-layout-spacer"}]
    [:nav {:class "mdl-navigation"}
    [:a {:class "mdl-navigation__link"
            :href "#/"
            } "Regattas"]
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
(defn boats-page [{regatta-id :regatta-id race-id :race-id}]
  [page boats-card regatta-id race-id])

;; Page routing 
(defn current-page []
  [:div [(session/get :current-page) (session/get :params)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")
(secretary/defroute "/" []
  (session/put! :current-page #'home-page))
(secretary/defroute "/regattas/:regatta-id/races" {:as params}
  (session/put! :current-page #'races-page)
  (session/put! :params params))

(secretary/defroute "/regattas/:regatta-id/races/:race-id/boats" {:as params}
  (session/put! :current-page #'boats-page)
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
