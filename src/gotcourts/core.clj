(ns gotcourts.core
  (:gen-class)
  (:require [org.httpkit.client :as http]
            [clojure.string :as str]
            [cheshire.core :refer :all]
            [clojure.edn :as edn]))


(defn- retrieve-data [call-fn url options response-fn]
  (let [response     (call-fn url options)
        return-value (response-fn @response)]
    return-value))

(defn homepage-cookie []
  (retrieve-data http/get "https://www.gotcourts.com/de/"
                 {} (fn [response] 
                      (let [set-cookie (->> response
                                            :headers
                                            :set-cookie)]
                        (first (str/split (first set-cookie) #";"))))))

(defn login-apikey [cookie]
  (retrieve-data http/post "https://www.gotcourts.com/de/api2/public/login/web"
                 {:headers     {"Cookie" cookie}
                  :form-params {:username "test@test123.com" 
                                :password "vivegotcourts"}}
                 (fn [response]
                   (->> response 
                        :body
                        (#(parse-string % true))
                        :response 
                        :apiKey))))

(defn court-data [cookie apikey court date]
  (retrieve-data http/get 
                 (str "https://www.gotcourts.com/de/api/secured/player/club/reservations/" court "?date=" date)
                 {:headers {"Cookie"      cookie 
                            "X-GOTCOURTS" (str "ApiKey=\"" apikey "\"")}}
                 (fn [response]
                   (->> response
                        :body
                        (#(parse-string % true))
                        :response))))


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
    (reverse free-slots)))

(defn- free-slots-by-court [reservations courts] 
  (let [stripped-reservations (map #(select-keys % [:startTime :endTime :courtId]) reservations)
        free-slots-by-court   (group-by :courtId stripped-reservations)
        court-by-id           (into {} (map (fn [court] [(:id court) court]) courts))
        free-slots            (into {} (map (fn [[court-id reservation]]
                                              (let [court (get court-by-id court-id)]
                                                (when-not (nil? court)
                                                  [court-id (get-free-slots reservation (:openingTime court) (:closingTime court))]))) 
                                            free-slots-by-court))]
    (vals (merge-with (fn [court free-slots] (assoc court :free-slots free-slots)) court-by-id free-slots))))

(defn extract-data [data]
  (assoc (select-keys (:club data) [:name :city :country :canonical-name :web :phone]) 
    :courts (free-slots-by-court (:reservations data) (courts-with-times (:courts (:club data))))))

(def core-fixtures
  {:hardhof (edn/read-string (slurp "fixtures/hardhof.edn"))
   :lengg   (edn/read-string (slurp "fixtures/lengg.edn"))})


