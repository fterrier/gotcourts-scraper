(ns app.scheduler
  (:require [chime :refer [chime-ch]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [clojure.core.async :as async :refer [<! go-loop]]
            [mount.core :refer [defstate]]))


(defn periodic-check [interval check-fn send-alert-fn]
  (let [chimes (chime-ch (rest (periodic-seq (t/now)
                                             (-> 5 t/seconds))))]
    (go-loop []
      (when-let [msg (<! chimes)]
        (prn "Chiming at:" msg)
        (recur)))
    chimes))

(defstate scheduler 
  :start (start-chimes )
  :stop (async/close! scheduler))


;; (defn gotcourtsjob [scheduler] 
;;   (fn []
;;     (let [data    (retrieve-raw-data (:scraper scheduler) "21" "2015-07-20")
;;           data    (extract-data data)]      
;;       (let [courts (:courts data)]
;;         (println (filter-free courts 54000 57600))))))


;; (defn start-job [scheduler job-definition]
;;   ""
;;   (println job-definition)
;;   (update-in scheduler [:jobs] #(conj % (schedule ((resolve (:function job-definition)) scheduler) 
;;                                                   (:schedule job-definition)))))

;; (defn stop-all-jobs [scheduler]
;;   ""
;;   (doseq [job (:jobs scheduler)]
;;     (stop job))
;;   (assoc scheduler :jobs nil))

;; (defrecord Scheduler [job-definitions scraper]
;;   component/Lifecycle
;;   (start [component]
;;          (println ";; Starting scheduler")
;;          ; TODO get job-definitions from database
;;          ; each job run should go like this;
;;          ; 1. get configured actions for this particular job definition
;;          ;    - user, settings, action
;;          ; 2. call job definition function (get the data)
;;          ; 3. call configured actions in a queue
;;          (reduce start-job component job-definitions))
;;   (stop [component]
;;         (println ";; Stopping scheduler")
;;         (stop-all-jobs component)))

;; (defn new-scheduler [job-definitions]
;;   (map->Scheduler {:job-definitions job-definitions}))
