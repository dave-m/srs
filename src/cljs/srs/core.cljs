(ns srs.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(def regattas (atom []))

(defn add-regatta! [name]
  (let [maxid (apply max (map :id @regattas))
        id (+ 1 (or maxid 0))]
    (swap! regattas conj
           {:id id
            :name name
            :details (str "Regatta " name)
            :races []})))

(defn regattas-card []
  (let [val (atom "")]
    (fn []
      [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
       [:div {:class "mdk-card mdl-cell mdl-cell--12-col"}
        [:h4 {:class "mdl-cell mdl-cell--12-col"} "Current Regattas"]
        [:div {:class "mdl-card__supporting-text mdl-grid mdl-grid--no-spacing"}
         (map (fn [r]
                [:div {:class "section__text mdl-cell mdl-cell--10-col-desktop mdl-cell--6-col-tablet mdl-cell--3-col-phone"}
                 [:h5 (:name r)]
                 [:p (str (:details r) ". See: ")
                  [:a {:href (str "#/races/" (:id r))} "races"]
                  (str ", ")
                  [:a {:href (str "#/boats/" (:id r))} "boats"]
                  (str ".")]])
              @regattas)]
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
                     :on-click #(when-let [r @val]
                                  (add-regatta! r)
                                  (reset! val ""))}
            [:i {:class "material-icons"} "add"]]]]]]])
    ))

(defn races-card [])
(defn boats-card [])

;; -------------------------
;; Views

(def current-view (atom regattas-card))
(defn home-page []
  [:div {:class "mdl-layout mdl-js-layout mdl-layout--fixed-header"}
   [:header {:class "mdl-layout__header"}
    [:div {:class "mdl-layout__header-row"}
     [:span {:class "mdl-layout-title"} "Sail Regatta Scoring"]
     [:span {:class "mdl-layout-spacer"}]
     [:nav {:class "mdl-navigation"}
      [:a {:class "mdl-navigation__link"
           :href "#/regattas"
           :on-click #(swap! @current-view regattas-card)} "Regatta"]
      [:a {:class "mdl-navigation__link"
           :href "#/races"
           :on-click #(swap! @current-view races-card)} "Races"]
      [:a {:class "mdl-navigation__link" :href "#/boats" :on-click #(swap! @current-view boats-card)} "Boats"]
      ]]]
   [:main {:class "mdl-layout__content page-content"}
    [:div {:class "mdl-grid"}
     [:div {:class "mdl-cell mdl-cell--12-col mdl-cell--9-col-tablet"} [@current-view]]]]])


(defonce init (do
                (add-regatta! "Nationals")
                (add-regatta! "Europeans")))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))


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

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
