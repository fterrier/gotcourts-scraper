(ns gotcourts.alerter
  (:require [clojure
             [data :as data]
             [edn :as edn]]
            [gotcourts.checker :as chk]))

(defn- gen-alert [alert-type slots id]
  (map (fn [slot] [alert-type {:slot slot :id id}]) slots))

(defn- alerts-from-diff [courts-before-map courts-after-map id]
  (let [[gone-slots new-slots _]
        (data/diff (:filtered-free-slots (get courts-before-map id))
                   (:filtered-free-slots (get courts-after-map id)))]
    (concat
     (gen-alert :new-slot new-slots id)
     (gen-alert :gone-slot gone-slots id))))

(defn- diff-free-slots [courts-before-map courts-after-map courts]
  (mapcat (fn [id]
         (alerts-from-diff courts-before-map courts-after-map id))
       courts))

(defn- to-map [courts]
  (->> courts 
       (group-by :id)
       (map (fn [[id courts]] [id (get courts 0)]))
       (into {})))

(defn get-alerts [courts-before courts-after]
  "- courts-before / courts-after
     A list of courts: [{:filtered-free-slots ...}]
   Returns a list of newly free slots."
  (let [courts-before-map (to-map courts-before)
        courts-after-map (to-map courts-after)
        all-courts (into #{} (concat (keys courts-before-map) 
                                     (keys courts-after-map)))]
    (concat
     (diff-free-slots courts-before-map courts-after-map all-courts))))
