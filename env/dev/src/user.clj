(ns user
  (:require [clojure.tools.namespace.repl :as tn]
            [app.scheduler :as scheduler]
            [app.server :as server]
            [mount.core :as mount]))

(defn go []
  (mount/start)
  :ready)

(defn stop []
  (mount/stop))

(defn reset []
  (mount/stop)
  (tn/refresh :after 'user/go))
