(ns gotcourts.bot-commands.show)

(defn- get-message [{:keys [success] :as message}]
  (case success
    :show {:message ""}
    :show-no-tasks {:message "At the moment you have no tasks. Use /notify to add a task."}))

(defn- render-tasks [user-tasks]
  (if (empty? user-tasks)
    {:success :show-no-tasks}
    {:success :show :options user-tasks}))

(defn handle-show* [tasks-db _ _ send-to-user-fn]
  (let [response (render-tasks @tasks-db)]  
    (send-to-user-fn 
     (assoc response :text (get-message response)))))

(defn create-show-command [tasks-db]
  (partial handle-show* tasks-db))
