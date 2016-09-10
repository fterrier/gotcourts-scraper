(ns user
  (:require [clojure.tools.namespace.repl :as tn]
            [app.scheduler :as scheduler]
            [app.server :as server]
            [mount.core :as mount]))

(def config 
  {:telegram {:key "272401208:AAFQ5oWsUKQk9-C9aOF6f2p2H7o47Yq8glE"
              :host "114530c8.ngrok.io"}})

(defn go []
  (-> (mount/swap {#'app.config/config config})
      (mount/with-args {:port 8000})
      mount/start)
  :ready)

(defn stop []
  (mount/stop))

(defn reset []
  (mount/stop)
  (tn/refresh :after 'user/go))
