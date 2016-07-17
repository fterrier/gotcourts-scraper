(ns scheduler.scheduler
  (:require [chime :refer [chime-ch]]
            [clj-time
             [core :as t]
             [periodic :refer [periodic-seq]]]
            [clojure.core.async :as async :refer [<! go go-loop]]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]))

(defn- execute-task [task-fn date old-data]
  (try 
    (task-fn date old-data)
    (catch Exception e (log/warn e "Error executing task"))))

(defn- periodic-check [interval task-fn]
  (log/info "Starting periodic check with interval" interval)
  (let [chimes (chime-ch (periodic-seq (t/now) interval))]
    (go-loop [old-data nil]
      (when-let [date (<! chimes)]
        (log/info "Chiming at:" date)
        (let [new-data (execute-task task-fn date old-data)]
          (recur new-data))))
    chimes))

(defn add-chime [scheduler interval task-fn]
  "Adds a chime, returns a function that, when called, stops the chime. 
   - interval: a clj-time interval of time between each task exec
   - task-fn: a function [date old-data]"
  (log/info "Starting chimes at interval" interval)
  (let [chime-ch (periodic-check interval task-fn)]
    (swap! scheduler conj chime-ch)
    (fn []
      (async/close! chime-ch)
      (swap! scheduler disj chime-ch))))

(defn stop-chimes [scheduler]
  (log/info "Stopping chimes")
  (doseq [chime @scheduler]
    (async/close! chime)))

(defn init-scheduler []
  (atom #{}))

