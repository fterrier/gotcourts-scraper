(ns gotcourts.bot-messages)

(defn unparse [{:keys [success error type options]}]
  (str (if success success error) options))
