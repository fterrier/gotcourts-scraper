(ns app.scheduler
  (:require [app.tasks :as tasks]
            [chime :refer [chime-ch]]
            [clj-time
             [core :as t]
             [periodic :refer [periodic-seq]]]
            [clojure.core.async :as async :refer [<! go go-loop]]
            [mount.core :refer [defstate]]))

(defn- call-and-alert [date old-data extract-fn send-alert-fn]
  (try 
    (when-let [data (extract-fn date)]
      (send-alert-fn old-data data)
      data)
    (catch Exception e 
      (println "Exception trying to execute task" e))))

(defn- periodic-check [interval extract-fn send-alert-fn]
  (let [chimes (chime-ch (rest (periodic-seq (t/now) interval)))]
    (go-loop [data nil]
      (when-let [msg (<! chimes)]
        (println "Chiming at:" msg)
        (let [data (call-and-alert msg data extract-fn send-alert-fn)]
          (recur data))))
    chimes))

(defn start-chimes [tasks]
  (println "Starting chimes" tasks)
  {:chimes
   (doall
    (for [{:keys [interval extract-fn send-alert-fn] :as task} tasks]
      (periodic-check interval extract-fn send-alert-fn)))})

(defn stop-chimes [scheduler]
  (println "Stopping chimes")
  (doseq [chime (:chimes scheduler)]
    (async/close! chime)))

(defstate scheduler
  :start (start-chimes tasks/tasks)
  :stop (stop-chimes scheduler))


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
