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
  (let [user             (:from message)
        [command & args] (parse-command (:text message))]
    {:user        user
     :command-key command
     :args        args}))

(defn handle-message* [commands data send-to-user-fn]
  (log/info "Incoming message for bot" data)
  (let [{:keys [user command-key args]} (parse-message data)
        command                         (get commands command-key)]
    (log/info "Got command - command-key" command-key "- args" args ":" command)
    (if-not (nil? command)
      ((:handle-fn command) user args (fn [response] (send-to-user-fn response command)))
      (send-to-user-fn {:error :command-not-found} nil))))

(defn- send-to-user-with-log-fn [send-to-user-fn response command]
  (log/info "Sending response to user" response)
  (send-to-user-fn response command))

(defn- index-by-name [commands]
  (->> commands
       (group-by :name)
       (map (fn [[name commands]] 
              (when (not= 1 (count commands))
                (throw (IllegalArgumentException. "Cannot have 2 commands with the same name.")))
              [name (first commands)]))
       (into {})))

(defn create-bot [commands]
  "Creates a bot with the given commands. Commands is a map of {<command-key> {:handle-fn ...}}.
   - <command-key>: the first chunk of a given command, e.g. /show or /notify
   - handler-fn: a function that takes as args [user args send-to-user-fn]"
  (let [commands-map (index-by-name commands)]
    (fn [data send-to-user-fn]
      (handle-message* commands-map data (partial send-to-user-with-log-fn send-to-user-fn)))))

