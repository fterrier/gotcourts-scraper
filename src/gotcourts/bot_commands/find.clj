(ns gotcourts.bot-commands.find)

(defn- handle-message* [scraper tasks-db user args send-to-user-fn]
  )

(defn create-find-command [scraper tasks-db]
  (partial handle-message* scraper tasks-db))
