(ns user
  (:require [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount]))

(defn go []
  (mount/start)
  :ready)

(defn reset []
  (mount/stop)
  (tn/refresh :after 'user/go))
