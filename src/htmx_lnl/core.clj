(ns htmx-lnl.core
  (:require
   [clojure.string :as str]
   [hiccup2.core :as h]
   [htmx-lnl.db :as db]
   [htmx-lnl.view :as view]
   [reitit.ring :as reitit]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.params :as params]
   [ring.middleware.reload :as reload]
   [ring.middleware.stacktrace :as stacktrace]
   [ring.util.response :as resp]))

(defn root-handler [_request]
  (resp/response (view/page-layout (view/trial-board (db/cards-by-stage db/card-db)))))

(defn edit-get-handler [request]
  (if-let [id (-> request :path-params :id parse-long)]
    (let [card (db/card-by-id db/card-db id)]
      (resp/response (view/page-layout (view/edit-card-form card))))
    (resp/bad-request "/")))

(defn card-errors [card]
  (cond-> {}
    (not (:id card))
    (assoc :id "Invalid id")
    (str/blank? (:title card))
    (assoc :title "Title is required")
    (str/includes? (:title card) "JIRA")
    (assoc :title "Thou speakest not of JIRA")
    (not (db/stages (keyword (:stage card))))
    (assoc :stage "Invalid stage")))

(defn new-get-handler [_request]
  (resp/response (view/page-layout (view/edit-card-form))))

(defn new-post-handler [request]
  (let [card (-> request
                 :form-params
                 (update-keys keyword)
                 (cond-> (contains? (:form-params request) "stage")
                   (update :stage keyword)))
        errors (dissoc (card-errors card) :id)]
    (if (seq errors)
      (resp/bad-request (view/page-layout (view/edit-card-form card errors)))
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
    (if (seq errors)
      (resp/bad-request (view/page-layout (view/edit-card-form card errors)))
      (do
        (db/update-card db/card-db card)
        (resp/redirect "/" :see-other)))))

(defn change-stage-handler [request]
  (let [{:keys [stage] :as card} (-> request
                                     :form-params
                                     (update-keys keyword)
                                     (update :id parse-long)
                                     (update :stage keyword))]
    (if (not (contains? db/stages stage))
      (resp/bad-request "Invalid stage")
      (do
        (db/update-card db/card-db card)
        (resp/response (str (h/html
                             [:div {:id (str "card-" (:id card))
                                    :hx-swap-oob "delete"}]
                             (view/kanban-column {:stage stage
                                                  :cards (db/cards-for-stage db/card-db stage)}))))))))

(defn delete-post-handler [request]
  (let [id (-> request :path-params :id parse-long)]
    (when id
      (db/delete-card-by-id db/card-db id))
    (resp/redirect "/" :see-other)))

(defn delete-handler [request]
  (let [id (-> request :path-params :id parse-long)]
    (when id
      (db/delete-card-by-id db/card-db id))
    (resp/response "")))

(defn validate-title-handler [request]
  (let [title (-> request :query-params (get "title"))
        error (:title (card-errors {:title title}))]
    (resp/response (or error ""))))

(def router
  (reitit/router
   [["/" {:get root-handler}]
    ["/create" {:get new-get-handler
                :post new-post-handler}]
    ["/change-stage" {:post change-stage-handler}]
    ["/:id/edit" {:get edit-get-handler
                  :post edit-post-handler}]
    ["/:id/delete" {:post delete-post-handler
                    :delete delete-handler}]
    ["/validate-title" {:get validate-title-handler}]]))

(def app (-> (reitit/ring-handler router)
             params/wrap-params
             stacktrace/wrap-stacktrace))

(defn main [& _args]
  (jetty/run-jetty (reload/wrap-reload #'app)
                   {:port 3000
                    :join? false}))

(comment
  (view/page-layout [:div])
  @db/card-db
  (db/update-card db/card-db {:id 8 :title "Craft mana potion" :stage :reviewing})
  (def server (main))
  (.stop server))
