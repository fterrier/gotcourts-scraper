(ns app.main
  (:gen-class)
  (:require [mount.core :as mount]
            [app.server :as server]
            [app.scheduler :as scheduler]
            [clojure.tools.cli :as tools]))

(defn parse-args [args]
  (let [opts [["-p" "--port [port]" "Port"
               :default 8000
               :parse-fn #(Integer/parseInt %)]]]
    (-> (tools/parse-opts args opts)
        :options)))

(defn -main [& args]
  (mount/start-with-args (parse-args args)))
