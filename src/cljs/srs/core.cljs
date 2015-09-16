(ns srs.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(def regattas (atom []))

(defn add-regatta! [name]
  (swap! regattas conj {:key name :name name}))

(defn regattas-table []
  (let [val (atom "")]
    (fn []
    [:section {:class "regatta-table section mdl-grid mdl-grid--no-spacing"}
     [:h4 {:class "mdl-cell mdl-cell--8-col header"} "Current Regatta's"]
     [:div {:class "mdl-cell mdl-cell--4-col"}
      [:div {:class "new mdl-textfield mdl-js-textfield mdl-textfield--floating-label"}
       [:input {:class "mdl-textfield__input"
                :type "text"
                :id "newRegatta"
                :value @val
                :on-change #(reset! val (-> % .-target .-value))}]
       [:label {:for "newRegatta" :class "mdl-textfield__label"} "New Regatta"]]
      [:div {:class "new"}
       [:button {:class "mdl-button mdl-js-button mdl-button--fab mdl-js-ripple-effect mdl-button--colored mdl-shadow--4dp mdl-color--accent"
                 :on-click #(when-let [r @val]
                            (add-regatta! r)
                            (reset! val ""))}
        [:i {:class "material-icons"} "add"]]]]
     [:table {:class "mdl-cell mdl-cell--12-col mdl-data-table mdl-js-data-table mdl-shadow--2dp"}
      [:thead [:tr
               [:th {:class "mdl-data-table__cell--non-numeric"} "Name"]
               [:th {:class "mdl-data-table__cell--non-numeric"} "Races"]
               [:th {:class "mdl-data-table__cell--non-numeric"} "Boats"]]]
      [:tbody
       (map (fn [r] [:tr
                   [:td {:class "mdl-data-table__cell--non-numeric"} (:name r)]
                     [:td [:a {:class "mdl-button mdl-button--colored action"} "View"]]
                     [:td [:a {:class "mdl-button mdl-button--colored action"} "View"]] ]) @regattas)]]])
  ))

(defn regattas-card []
  "Card displaying up to 5 of the current Regattas"
  (let [regattas (take 5 @regattas)]
    [:div {:class "home-card mdl-card mdl-shadow--2dp"}
           [:div {:class "mdl-card__title"}
            [:h2 {:class "mdl-card__title-text"} "Regattas"]]
           [:div {:class "mdl-card__supporting-text"}
            (str "All currently active Regattas: "
                 (clojure.string/join ", " (map :name regattas))
                 (if (= (count regattas) 5) "..."))]
           [:div {:class "mdl-card__actions mdl-card--border"}
            [:a {:class "mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"} "View"]]]))

(defn boats-card []
  [:div {:class "home-card mdl-card mdl-shadow--2dp"}
   [:div {:class "mdl-card__title"}
    [:h2 {:class "mdl-card__title-text"} "Boats"]]
   [:div {:class "mdl-card__supporting-text"} "All current Boats"]
   [:div {:class "mdl-card__actions mdl-card--border"}
    [:a {:class "mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"} "View"]]])

(defn races-card []
  [:div {:class "home-card mdl-card mdl-shadow--2dp"}
   [:div {:class "mdl-card__title"}
    [:h2 {:class "mdl-card__title-text"} "Races"]]
   [:div {:class "mdl-card__supporting-text"} "All currently active Races"]
   [:div {:class "mdl-card__actions mdl-card--border"}
    [:a {:class "mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"} "View"]]])

;; -------------------------
;; Views

(defn home-page []
  [:div {:class "mdl-layout mdl-js-layout"}
   [:header {:class "mdl-layout__header mdl-layout__header--scroll"}
    [:div {:class "mdl-layout__header-row"}
     [:span {:class "mdl-layout-title"} "Sail Regatta Scoring"]
     [:span {:class "mdl-layout-spacer"}]
     [:nav {:class "mdl-navigation"}
       [:a {:class "mdl-navigation__link" :href "#/regattas"} "Regatta"]
       [:a {:class "mdl-navigation__link" :href "#/boats"} "Boats"]
      [:a {:class "mdl-navigation__link" :href "#/races"} "Races"]]]]
   [:main {:class "mdl-layout__content page-content"}
    [:div {:class "mdl-grid"}
     [:div {:class "mdl-cell mdl-cell--8-col"} [regattas-table]]
     [:div {:class "mdl-cell mdl-cell--4-col"}
      [:div [regattas-card] ]
      [:div [races-card]]
      [:div [boats-card]]]]]])


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
