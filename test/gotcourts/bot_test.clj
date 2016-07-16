(ns gotcourts.bot-test
  (:require [clojure.test :refer [deftest is testing]]
            [gotcourts.bot :as bot]))

(deftest bot-test
  (testing "Error outputs error message and no db change"
    (let [[response tasks] 
          (bot/handle-command nil {} {:error {:text "error"}})]
      (is (= "error" response))
      (is (= {} tasks)))))
