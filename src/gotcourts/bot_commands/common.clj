(ns gotcourts.bot-commands.common
  (:require [gotcourts
             [scraper :as scraper]
             [task :as task]]))

(defn add-venue-map [scraper {:keys [venues] :as command}]
  "Given a list of venue terms, retrieves the associated venue ids."
  (let [venue-map     (task/fetch-gotcourts-venues (partial scraper/fetch-venues scraper) venues)
        chosen-venues (->> venue-map
                           (map (fn [[_ venues]] (first venues)))
                           (remove nil?))]
    (assoc command :chosen-venues chosen-venues)))
