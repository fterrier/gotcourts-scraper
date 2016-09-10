(ns telegram.handler
  (:require [clojure.core.async :refer [<! >!! chan go go-loop]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [POST routes]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [telegram.client :as client]))

(defn- register-webhook [webhook-domain bot-id]
  @(client/set-webhook bot-id (str "https://" webhook-domain "/telegram")))

(defn- start-handler-loop [message-ch handler]
  (go-loop []
    (when-let [{:keys [body channel] :as message} (<! message-ch)]
      (try 
        (handler body channel)
        (catch Exception e (log/warn e "Error handling message" message)))
      (recur))))

(defn- start-client-loop [client-ch bot-id chat-id]
  (go-loop []
    (when-let [{:keys [message options]} (<! client-ch)]
      @(client/send-message bot-id chat-id message options)
      (recur))))

(defn- client-channel* [chat-id client-atom bot-id]
  (when-not (get @client-atom chat-id)
    (let [client-ch (chan)]
      (swap! client-atom assoc chat-id client-ch)
      (start-client-loop client-ch bot-id chat-id)))
  (get @client-atom chat-id))

(defn- event-msg-handler* [message-ch client-atom bot-id]
  (fn [{:as ev-msg :keys [body]}]
    (log/debug "Got message from telegram client" body)
    (let [{{:keys [text chat from]} :message} body
          client-ch            (client-channel* (:id chat) client-atom bot-id)]
      (>!! message-ch {:body body :channel client-ch}))))

(defn- app-routes [msg-handler]
  (routes
   ;; (-> (GET "/inspect/telegram" [request] {:body (convert-to-json @games)})
   ;;     wrap-json-response)
   (-> (POST "/telegram" request (msg-handler request) {:body "ok"})
       (wrap-json-body {:keywords? true :bigdecimals? true})
       wrap-json-response)))

(defn app [bot-id webhook-domain handler]
  "The handler is a function that takes the following arguments:
   - body : the original request body
   - channel : how to talk back to the client"
  (log/info "Starting telegram handler with " bot-id webhook-domain)
  (let [message-ch  (chan 10)
        client-atom (atom {})
        msg-handler (event-msg-handler* message-ch client-atom bot-id)]
    (go
      (when webhook-domain (register-webhook webhook-domain bot-id))
      (start-handler-loop message-ch handler))
    (app-routes msg-handler)))

;; TODO function to stop this
