(ns app.database
  (:require [mount.core :refer [defstate]]))

(defstate db :start (atom {}))
