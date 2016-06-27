(ns gotcourts.checker
  (:require [clojure.edn :as edn]))


(defn- filter-free-slots [free-slots starttime endtime]
  (->> free-slots
       (filter #(and
                 (and (<= starttime (:endTime %)) (>= starttime (:startTime %))) 
                 (and (>= endtime (:startTime %)) (<= endtime (:endTime %)))))
       (into [])))

(defn filter-free [courts starttime endtime]
  (->> courts
       (map #(assoc % :filtered-free-slots 
                    (filter-free-slots (:free-slots %) starttime endtime)))
       (remove #(= 0 (count (:filtered-free-slots %))))
       (into [])))

(defn- courts-with-times [courts]
  (map #(select-keys % [:id :label :surfaceType :type :openingTime :closingTime]) courts))

(defn- get-free-slots [busy-slots opening closing]
  (let [sorted-busy-slots    (sort-by :startTime busy-slots)
        uncleaned-free-slots (reduce 
                               (fn [[first-free & rest-free :as free-slots] 
                                    {:keys [startTime endTime] :as reservation}]
                                 ; we begin a free time slot at the end of the period
                                 (cons {:startTime endTime :endTime endTime} 
                                       (if (> startTime (:endTime first-free))
                                         ; we extend the preceding free time slot
                                         (cons (assoc first-free :endTime startTime) rest-free)
                                         ; otherwise it's not extended
                                         free-slots)))
                               [{:startTime opening :endTime opening}] sorted-busy-slots)
        bounded-free-slots   (cons (assoc (first uncleaned-free-slots) :endTime closing) 
                                   (rest uncleaned-free-slots))
        free-slots           (remove #(= (:startTime %) (:endTime %)) bounded-free-slots)]
    (into [] (reverse free-slots))))

(defn- free-slots-by-court [reservations courts] 
  (let [stripped-reservations (map #(select-keys % [:startTime :endTime :courtId]) reservations)
        free-slots-by-court   (group-by :courtId stripped-reservations)
        court-by-id           (into {} (map (fn [court] [(:id court) court]) courts))
        free-slots            (into {} (map (fn [[court-id reservation]]
                                              (let [court (get court-by-id court-id)]
                                                (when-not (nil? court)
                                                  [court-id (get-free-slots reservation 
                                                                            (:openingTime court) 
                                                                            (:closingTime court))]))) 
                                            free-slots-by-court))]
    (into [] 
          (vals (merge-with (fn [court free-slots] (assoc court :free-slots free-slots)) court-by-id free-slots)))))

(defn extract-data [data]
  (assoc (select-keys (:club data) [:name :city :country :canonical-name :web :phone]) 
    :courts (free-slots-by-court (:reservations data) (courts-with-times (:courts (:club data))))))
