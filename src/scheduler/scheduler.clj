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

(defn- periodic-check [scheduler {:keys [interval until]} task-fn]
  (log/info "Starting periodic check with interval" interval)
  ;; we add a few milliseconds otherwise the first chime might be skipped
  (let [chimes (chime-ch (apply periodic-seq (remove nil? [(t/plus (t/now) (t/millis 100)) until interval])))]
    (go-loop [old-data nil]
      (let [date (<! chimes)]
        (when-not date 
          (swap! scheduler disj chimes))
        (when date
          (log/info "Chiming at:" date)
          (let [new-data (execute-task task-fn date old-data)]
            (recur new-data)))))
    (swap! scheduler conj chimes)
    chimes))

(defn add-chime [scheduler {:keys [interval until] :as options} task-fn]
  "Adds a chime, returns a function that, when called, stops the chime. 
   - interval: a clj-time interval of time between each task exec
   - task-fn: a function [date old-data]"
  (log/info "Starting chimes at interval" interval)
  (let [chime-ch (periodic-check scheduler options task-fn)]
    (fn []
      (async/close! chime-ch))))

(defn stop-chimes [scheduler]
  (log/info "Stopping chimes")
  (doseq [chime @scheduler]
    (async/close! chime)))

(defn init-scheduler []
  (atom #{}))

