(ns telegram.client
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [org.httpkit.client :as http]))

;; START TELEGRAM CLIENT
(defn- send-telegram [bot-id command options]
  (let [url (str "https://api.telegram.org/bot" bot-id "/" command)]
    (log/info "Telegram - Sending message" url options)
    (http/get url options
              (fn [{:keys [status headers body error]}]
                (if (or error (not= status 200))
                  (log/error "Telegram - Failed, exception is " error status body)
                  (log/info "Telegram - Async HTTP GET: " status body))))))

(defn- get-telegram-options [{:keys [parse-mode reply-keyboard message-id] 
                              :as options}]
  "Options can be :
    - parse-mode: :markdown or nil
    - reply-keyboard: a vector of vector"
  (cond-> {}
    parse-mode (assoc :parse_mode (name parse-mode))
    message-id (assoc :reply_to_message_id message-id)
    reply-keyboard (assoc :reply_markup (json/encode {:keyboard reply-keyboard}))))

(defn send-message 
  ([bot-id chat-id text options]
   (let [telegram-options (get-telegram-options options)]
     (send-telegram bot-id "sendMessage" {:query-params (merge {:chat_id chat-id :text text} telegram-options)})))
  ([bot-id chat-id text]
   (send-message bot-id chat-id text {})))

(defn get-me [bot-id]
  (send-telegram bot-id "getMe" nil))

(defn set-webhook [bot-id url]
  (send-telegram bot-id "setWebhook" {:query-params {:url url}}))

(comment 
  @(send-message "152122841:AAE4iFW3JNdANZ0lPpibZ5pf-vYmH5z-p2w" "@fterrier" "test")
  @(get-me       "152122841:AAE4iFW3JNdANZ0lPpibZ5pf-vYmH5z-p2w")
  @(set-webhook  "152122841:AAE4iFW3JNdANZ0lPpibZ5pf-vYmH5z-p2w" "https://73ed7de4.ngrok.io/telegram"))
