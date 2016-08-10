(ns gotcourts.bot-commands.command
  (:require [bot.command-parser :as parser]))

(defn- parse-command-chunks [type-format-args args]
  "Given a list of [<type> <format>] args and a list of args, returns the given command."
  (let [format-args         (map second type-format-args)
        type-args           (map first type-format-args)
        [parsed-args error] (parser/parse-command-chunks format-args args)]
    (if error
      [nil error]
      (reduce (fn [[command error] [type value]]
                (if (nil? value) 
                  (reduced [nil {:error :format-error :type type}])
                  [(assoc command type value) nil]))
              [{} nil] (map vector type-args parsed-args)))))

(defn- get-format-error-message [type type-message-map format-message]
  (println type-message-map)
  {:message (str 
             (case type
               :too-many-args   "Wrong number of arguments."
               :not-enough-args "Wrong number of arguments."
               (get type-message-map type))
             " " format-message)
   :options {:parse-mode :markdown}})

(defn create-command [handle-fn type-format-message-args format-message]
  (fn [user args send-to-user-fn]
    (let [[command error] (parse-command-chunks type-format-message-args args)]
      (if error 
        (send-to-user-fn (assoc error :text (get-format-error-message 
                                             (:type error)
                                             (->> type-format-message-args
                                                  (map (fn [[type _ message]] [type message]))
                                                  (into {}))
                                             format-message)))
        (handle-fn user command send-to-user-fn)))))
