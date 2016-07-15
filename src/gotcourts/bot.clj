(ns gotcourts.bot
  (:require [clojure.core.async :refer [>!!]]))

(defn- call-and-alert [date old-data extract-fn send-alert-fn]
  (when-let [data (extract-fn date)]
    (send-alert-fn old-data data)
    data))



(defn create-bot [scheduler]
  (fn [data ch]
    (>!! ch data)))

;; [(task/create-gotcourts-task app/scraper println 
;;                              [15 16 17] (t/minutes 5) 50400 60000)]
