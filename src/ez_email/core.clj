(ns ez-email.core
  (:require [clojure.core.async :refer [<! >! chan close! go go-loop sliding-buffer]]
            [clojure.string :as str]
            [postal.core :as postal]))


(defonce -channel (atom nil))
(defonce -provider (atom nil))

(defprotocol IProvider
  (message-sent [provider message result])
  (send-message [provider message]))

(extend-type nil
  IProvider
  (send-message [this _]
    (throw (ex-info "Tried to send a message on nil"
                    {:what ::send-message}))))

(defrecord PostalProvider [host port user password ssl? mime from postal c-in c-result]
  IProvider
  (send-message [this message]
    (let [result (postal/send-message (:postal this)
                                      (merge {:from from}
                                             message))]
      (message-sent this message result)))
  (message-sent [this message result]
    (go (>! c-result {:message message
                      :result result}))))

(defn queue-message
  ([data]
     (go
       (>! @-channel data)))
  ([to subject body]
     (go
       (>! @-channel {:to to
                     :subject subject
                     :body body})))
  ([from to subject body]
     (go
       (>! @-channel {:from from
                     :to to
                     :subject subject
                     :body body}))))

(defn shutdown! []
  (when-let [c (get-in @-provider [:c-in])]
    (close! c))
  (when-let [c (get-in @-provider [:c-result])]
    (close! c))
  (reset! -channel nil)
  (reset! -provider nil))

(defn- get-default-opts [opts]
  (let [c-in (or (:c-in opts) (chan))
        c-result (or (:c-result opts) (chan (sliding-buffer 1)))
        postal (or (:postal opts)
                   {:host (:host opts)
                    :port (:port opts)
                    :user (:user opts)
                    :pass (:password opts)
                    :ssl (:ssl? opts)})]
    (merge
     {:init? true
      :c-in c-in
      :c-result c-result
      :postal postal}
     opts)))

(defn- init [provider opts]
  (when (true? (:init? opts))
    (reset! -provider provider)
    (reset! -channel (:c-in provider))
    ;; setup channel in
    (let [c-in (:c-in provider)]
     (go-loop []
       (when-let [msg (<! c-in)]
         (send-message provider msg)
         (recur))))))

(defn get-provider
  ([opts]
   (let [opts (get-default-opts opts)
         provider (map->PostalProvider opts)]
     (init provider opts)
     provider))
  ([host user password]
   (let [opts (get-default-opts {:host host :user user :password password})
         provider (map->PostalProvider opts)]
     (init provider opts)
     provider))
  ([host port user password ssl? mime from]
   (let [opts (get-default-opts {:host host :port port :user user :password password :ssl? ssl? :mime mime :from from})
         provider (map->PostalProvider opts)]
     (init provider opts)
     provider)))
