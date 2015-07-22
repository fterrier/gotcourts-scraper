(ns app.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [app.system :as app]))


(defn -main [& args]
  (let [[port] args]
    (component/start
      (app/system 
        {:port (Integer. (or (System/getenv "PORT") "8080"))}))))