(ns app.main
  (:gen-class)
  (:require [mount.core :as mount]))

(defn -main [& args]
  (let [[port] args]
    (mount/start)))
