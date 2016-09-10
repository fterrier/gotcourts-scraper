(ns app.config
  (:require [mount.core :refer [defstate]]))

(defstate config :start 
  {:telegram {:key "244753532:AAEm7RIud0nszMYOWepyE5lJpQwVt8bm1D8"
              :host "telegramtennisbot.herokuapp.com"}})
