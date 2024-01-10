(ns htmx-lnl.view
  (:require
   [hiccup2.core :as h]
   [htmx-lnl.db :as db]))

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
                :crossorigin "anonymous"}]
      [:script {:src "https://unpkg.com/htmx.org@1.9.10"
                :crossorigin "anonymous"}]]
     [:body {:hx-boost "true"}
      [:section.section
       content]]])))

(defn kanban-card [card]
  [:div.card {"hx-on:dragstart" (format "event.dataTransfer.setData('text/plain', '%s')" (:id card))
              :draggable "true"
              :id (str "card-" (:id card))}
   [:header.card-header
    [:p.card-header-title (:title card)]
    [:button.card-header-icon
     [:a {:href (str "/" (:id card) "/edit")}
      [:span.icon
       [:i.fa-solid.fa-pencil]]]]]
   [:div.card-content
    [:a. {:hx-delete (str "/" (:id card) "/delete")
          :hx-target "closest div.card"
          :hx-swap "outerHTML"}
     "Delete"]]])

(defn kanban-column [{:keys [stage cards]}]
  (into [:div.column {"hx-on:dragover" "event.preventDefault(); event.dataTransfer.dropEffect = 'move'"
                      :hx-trigger "drop"
                      :hx-post "/change-stage"
                      :hx-vals (format "js:{\"id\": event?.dataTransfer?.getData('text/plain'), \"stage\": \"%s\"}" (name stage))
                      :hx-target "this"
                      :hx-swap "outerHTML"}
         [:h2.subtitle.has-text-centered (get db/stages stage)]] (map kanban-card) cards))

(defn trial-board [cards-by-stage]
  [:div
   [:a.button.is-link.is-info {:href "/create"} "Create new card"]
   (into [:div.columns.mt-3]
         (map (fn [stage] (kanban-column {:stage stage :cards (get cards-by-stage stage)})))
         (keys db/stages))])

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
      [:input.input {:form "edit-form"
                     :type "text"
                     :name "title"
                     :placeholder "Card title"
                     :value (:title card "")
                     :hx-get "/validate-title"
                     :hx-target "next p.help.is-danger"}]]
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
