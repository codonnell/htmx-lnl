(ns htmx-lnl.db)

(def stages {:ready-for-dev "Ready for Development"
             :developing "Developing"
             :ready-for-review "Ready for Review"
             :reviewing "Reviewing"
             :ready-to-accept "Ready to Accept"
             :accepting "Accepting"
             :done "Done"})

(defn cards-by-stage [db]
  (->> @db
       vals
       (sort-by :title)
       (group-by :stage)))

(defn cards-for-stage [db stage]
  (->> @db
       vals
       (filter #(= stage (:stage %)))
       (sort-by :title)))

(defn update-card [db card]
  (swap! db update (:id card) merge card))

(defn card-by-id [db id]
  (get @db id))

(defn delete-card-by-id [db id]
  (swap! db dissoc id))

(defn create-card [db card]
  (let [id (inc (apply max (keys @db)))]
    (swap! db assoc id (assoc card :id id))))

(def card-db
  (atom
   {1 {:id 1
       :title "Build frobnicator"
       :stage :ready-for-dev}
    2 {:id 2
       :title "Learn fireball spell"
       :stage :developing}
    3 {:id 3
       :title "Learn ice bolt spell"
       :stage :ready-for-review}
    4 {:id 4
       :title "Save prince in distress"
       :stage :ready-for-dev}
    5 {:id 5
       :title "Collect 10 herbs"
       :stage :done}
    6 {:id 6
       :title "Lay down the hammer of justice"
       :stage :reviewing}
    7 {:id 7
       :title "Escape terrible maze of doom"
       :stage :accepting}
    8 {:id 8
       :title "Craft healing potion"
       :stage :reviewing}}))
