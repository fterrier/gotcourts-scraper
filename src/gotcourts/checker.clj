(ns gotcourts.checker
  (:require [clojure.edn :as edn]))


(defn- filter-free-slots [free-slots starttime endtime]
  (filter #(or 
             (and (<= starttime (:endTime %)) (>= starttime (:startTime %))) 
             (and (>= endtime (:startTime %)) (<= endtime (:endTime %)))) 
          free-slots))

(defn filter-free [courts starttime endtime]
  (remove #(= 0 (count (:filtered-free-slots %)))
          (map #(assoc % 
          :filtered-free-slots (filter-free-slots (:free-slots %) starttime endtime)) 
       courts)))


(def checker-fixtures
  {:hardhof (edn/read-string (slurp "fixtures/extracted/hardhof.edn"))})