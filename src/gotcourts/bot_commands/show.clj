(ns gotcourts.bot-commands.show)

(defn- render-tasks [user-tasks]
  (if (empty? user-tasks)
    {:success :show-no-tasks}
    {:success :show :options user-tasks}))

(defn handle-show* [tasks-db _ _ send-to-user-fn]
  (send-to-user-fn (render-tasks @tasks-db)))

(defn create-show-command [tasks-db]
  (partial handle-show* tasks-db))
