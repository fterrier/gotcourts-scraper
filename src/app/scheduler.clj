(ns app.scheduler
  (:require [com.stuartsierra.component :as component]
            [gotcourts.scraper :refer :all]
            [gotcourts.checker :refer :all]
            [immutant.scheduling :refer :all]))

(defn gotcourtsjob [scraper]
  (fn []
    (let [data    (retrieve-raw-data scraper "21" "2015-07-20")
          data    (extract-data data)]
      
      (let [courts (:courts data)]
        (println (filter-free courts 54000 57600))))))

; (defn add-job [scheduler type schedule scraper]
;   (let [
;         ; job     (j/build
;         ;           (j/of-type type))
;         job     (->NoOpJob scraper)
;         job     (j/finalize job)
;         trigger (t/build
;                   (t/start-now)
;                   (t/with-schedule schedule))]
;     (qs/schedule scheduler job trigger)))

; (defn initialize [scheduler scraper]
;   (add-job scheduler NoOpJob (schedule (with-interval-in-seconds 5)) scraper))

(defrecord Scheduler [job scraper]
  component/Lifecycle
  (start [component]
         (println ";; Starting scheduler")
         (let [job 
               (schedule (gotcourtsjob scraper)
                         (-> (every 5 :second)))]
           (assoc component :job job)))
  (stop [component]
        (println ";; Stopping scheduler")
        (stop job)
        (dissoc component :job)))
