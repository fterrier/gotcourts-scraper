(ns bot.bot
  (:require [clojure.core.async :refer [>!!]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn- parse-command [text]
  (str/split (str/trim text) #"\s+"))

;; TODO handle edited-message
(defn- parse-message [{:keys [message] :as data}]
  "Takes a telegram message and parses it to {:user ... :command-key ... :error ...}"
  (log/info "Parsing message" message)
  (let [user (:from message)
        args (parse-command (:text message))]
    {:user user
     :args args}))

(defn handle-message* [commands data send-to-user-fn]
  (log/info "Incoming message for bot" data)
  (let [{:keys [user args]} (parse-message data)
        command             (some (fn [{:keys [match-fn] :as command}] 
                                    (when (match-fn args) command)) commands)]
    (log/info "Got command for args\"" args ":" command)
    (if-not (nil? command)
      ((:handle-fn command) user args (fn [response] (send-to-user-fn response command)))
      (send-to-user-fn {:error :command-not-found} nil))))

(defn- send-to-user-with-log-fn [send-to-user-fn response command]
  (log/info "Sending response to user" response)
  (send-to-user-fn response command))

(defn create-bot [commands]
  "Creates a bot with the given commands. Commands is a list of {:match-fn ... :handle-fn ...}}.
   - match-fn: a function that returns true if the command should match
   - handler-fn: a function that takes as args [user args send-to-user-fn]"
  (fn [data send-to-user-fn]
    (handle-message* commands data (partial send-to-user-with-log-fn send-to-user-fn))))

