# ez-email

A Clojure library designed to make email easy. There are lots of good alternatives out there for email, but few of them have been built with extensionability in mind.

## Usage

```clojure
(ns scratch
  (:require [clojure.core.async :as async]
            [ez-email.core :as email]))

(let [provider-data {;; this is the default from
                     :from "test@example.com"
                     ;; this is data for postal
                     :user "test@example.com"
                     :password "my password"
                     :ssl? true
                     :host "smtp.example.com"}
      provider (email/get-provider provider-data)]
  (println "Init done")
  (let [c (:c-result provider)]
    (async/go-loop []
      (when-let [v (async/<! c)]
        (println "Got the result from sending the message")
        (println v)
        (recur))))
  (email/queue-message "emil@emil0r.com" "This is a test from ez-email" "nt")
  (println "Sleeping...")
  (Thread/sleep 4000)
  (println "Shutting down core.async channel")
  (email/shutdown!))

```

## License

Copyright © 2016 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

---

Coram Deo
