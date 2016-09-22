(ns ez-email.core
  (:require [clojure.core.async :as async :refer [go chan close! <! >!]]
            [clojure.string :as str]
            [postal.core :as postal]))


(defonce ^:private channel (atom nil))

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
       (>! @channel data)))
  ([to subject body]
     (go
       (>! @channel {:to to
                     :subject subject
                     :body body})))
  ([from to subject body]
     (go
       (>! @channel {:from from
                     :to to
                     :subject subject
                     :body body}))))

(defn- get-default-opts [opts]
  (let [c-in (or (:c-in opts) (chan))
        c-result (or (:c-result opts) (chan (async/sliding-buffer 1)))]
    {:c-in c-in
     :c-result c-result}))

(defn get-provider
  ([opts]
   (map->PostalProvider (merge (get-default-opts opts)
                               opts)))
  ([host user password]
   (map->PostalProvider (merge (get-default-opts nil)
                               {:host host :user user :password password})))
  ([host port user password ssl? mime from]
   (map->PostalProvider (merge (get-default-opts nil)
                               {:host host :port port :user user :password password
                                :ssl? ssl? :mime mime :from from}))))
