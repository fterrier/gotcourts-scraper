(ns app.scheduler
  (:require [mount.core :refer [defstate]]
            [scheduler.scheduler :as scheduler]))

(defstate scheduler
  :start (scheduler/init-scheduler)
  :stop (scheduler/stop-chimes scheduler))
