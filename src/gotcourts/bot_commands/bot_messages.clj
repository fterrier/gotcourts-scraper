(ns gotcourts.bot-commands.bot-messages)

(defn- get-default [message]
  {:message (str message)})

(defmulti unparse-error (fn [{:keys [error]} _] error))
(defmulti unparse-success (fn [{:keys [success]} _] success))

(defmethod unparse-error :command-not-found [_ _]
  {:message "Sorry, did not understand this. Use /help to get help."})

(defmethod unparse-error :format-error [message {:keys [name]}]
  (case name
    "/notify" {:message "PROUT"}
    :default (get-default message)))

(defmethod unparse-success :show [_ {:keys [name]}]
  )

(defmethod unparse-success :show-no-tasks [_ _]
  {:message "At the moment you have no tasks. Use /notify to add a task."})




(defmethod unparse-error :default [message _]
  (get-default message))

(defmethod unparse-success :default [message _]
  (get-default message))

(defn unparse [{:keys [success error type options] :as message} command]
  ;{:message (str message)}
  (if success 
    (unparse-success message command)
    (unparse-error message command)))
