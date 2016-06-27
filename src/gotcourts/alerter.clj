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

(defn get-alerts [courts-before-map courts-after-map]
  "- courts-before / courts-after
     A list of courts with {:filtered-free-slots ..}
   Returns a list of newly free slots."
  (let [all-courts (into #{} (concat (keys courts-before-map) 
                                     (keys courts-after-map)))]
    (concat
     (diff-free-slots courts-before-map courts-after-map all-courts))))

