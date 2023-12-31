(ns htmx-lnl.core
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [htmx-lnl.db :as db]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [ring.middleware.reload :as reload]
            [ring.util.response :as resp]
            [reitit.ring :as reitit]))

(defn page-layout [content]
  (str
   (h/html
    {:mode :html}
    (h/raw "<!DOCTYPE html>")
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      [:title "Hello World"]
      [:link {:rel "stylesheet"
              :href "https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css"}]
      [:script {:src "https://kit.fontawesome.com/9f53493378.js"
                :crossorigin "anonymous"}]]
     [:body
      [:section.section
       content]]])))

(defn kanban-card [card]
  [:div.card
   [:header.card-header
    [:p.card-header-title (:title card)]
    [:button.card-header-icon
     [:a {:href (str "/" (:id card) "/edit")}
      [:span.icon
       [:i.fa-solid.fa-pencil]]]]]])

(defn kanban-column [{:keys [stage cards]}]
  (into [:div.column [:h2.subtitle.has-text-centered (get db/stages stage)]] (map kanban-card) cards))

(defn trial-board [cards-by-stage]
  [:div
   [:a.button.is-link.is-info {:href "/create"} "Create new card"]
   (into [:div.columns.mt-3]
         (map (fn [stage] (kanban-column {:stage stage :cards (get cards-by-stage stage)})))
         (keys db/stages))])

(defn root-handler [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (page-layout (trial-board (db/cards-by-stage db/card-db)))})

(defn edit-card-form
  ([]
   (edit-card-form {} {}))
  ([card]
   (edit-card-form card {}))
  ([card errors]
   [:div
    [:form#edit-form {:method "post"}]
    (when (:id card)
      [:form#delete-form {:method "post" :action (str "/" (:id card) "/delete")}])

    [:div.field
     [:label.label "Title"]
     [:div.control
      [:input.input {:form "edit-form" :type "text" :name "title" :placeholder "Card title" :value (:title card "")}]]
     [:p.help.is-danger (get errors :title)]]
    [:div.field
     [:label.label "Stage"]
     [:div.control
      [:div.select
       (into [:select {:name "stage" :form "edit-form"}]
             (map (fn [[k v]]
                    [:option (cond-> {:value k}
                               (= k (:stage card))
                               (assoc :selected true)) v]))
             db/stages)]]
     [:p.help.is-danger (get errors :stage)]]
    [:div.field.is-grouped
     [:div.control
      [:button.button.is-link {:form "edit-form"} "Submit"]]
     [:div.control
      [:a.button.is-link.is-light {:href "/"} "Cancel"]]
     (when (:id card)
       [:div.control
        [:button.button.is-danger {:form "delete-form"} "Delete"]])]]))

(defn edit-get-handler [request]
  (if-let [id (-> request :path-params :id parse-long)]
    (let [card (db/card-by-id db/card-db id)]
      (pp/pprint {:id id :card card})
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (page-layout (edit-card-form card))})
    (resp/bad-request "/")))

(defn card-errors [card]
  (cond-> {}
    (not (:id card))
    (assoc :id "Invalid id")
    (str/blank? (:title card))
    (assoc :title "Title is required")
    (not (db/stages (keyword (:stage card))))
    (assoc :stage "Invalid stage")))

(defn new-get-handler [_request]
  (resp/response (page-layout (edit-card-form))))

(defn new-post-handler [request]
  (let [card (-> request
                 :form-params
                 (update-keys keyword)
                 (cond-> (contains? (:form-params request) "stage")
                   (update :stage keyword)))
        errors (dissoc (card-errors card) :id)]
    (if (seq errors)
      (resp/bad-request (page-layout (edit-card-form card errors)))
      (do (db/create-card db/card-db card)
          (resp/redirect "/" :see-other)))))

(defn edit-post-handler [request]
  (let [id (get-in request [:path-params :id])
        card (-> request
                 :form-params
                 (update-keys keyword)
                 (assoc :id (parse-long id))
                 (cond-> (contains? (:form-params request) "stage")
                   (update :stage keyword)))
        errors (card-errors card)]
    (pp/pprint {:card card :errors errors :path-params (:path-params request)})
    (if (seq errors)
      (resp/bad-request (page-layout (edit-card-form card errors)))
      (do
        (db/update-card db/card-db card)
        (resp/redirect "/" :see-other)))))

(defn delete-post-handler [request]
  (let [id (-> request :path-params :id parse-long)]
    (when id
      (db/delete-card-by-id db/card-db id))
    (resp/redirect "/" :see-other)))

(def router
  (reitit/router
   [["/" {:get root-handler}]
    ["/create" {:get new-get-handler
                :post new-post-handler}]
    ["/:id/edit" {:get edit-get-handler
                  :post edit-post-handler}]
    ["/:id/delete" {:post delete-post-handler}]]))

(def app (-> (reitit/ring-handler router)
             params/wrap-params))

(defn main [& _args]
  (jetty/run-jetty (reload/wrap-reload #'app)
                   {:port 3000
                    :join? false}))

(comment
  (page-layout [:div])
  @db/card-db
  (db/update-card db/card-db {:id 8 :title "Craft mana potion" :stage :reviewing})
  (def server (main))
  (.stop server))
