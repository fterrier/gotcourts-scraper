(ns gotcourts.bot-commands.help)

(defn- get-message []
  {:message "Welcome to BookTennisBot. The following commands are available:.\n
  - /find : look for available venues
  - /help : show this message" 
   :options {:parse-mode :markdown}})

(defn handle-help* [{:keys [user]} send-to-user-fn]
  (send-to-user-fn {:text (get-message)}))

(defn create-help-command []
  (partial handle-help*))
