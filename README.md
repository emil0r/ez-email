# ez-email

A Clojure library designed to wrap the sending of email. Easy to extend with new providers and keep the same semantics.

## Usage

```clojure
(ns scratch
  (:require [clojure.core.async :as async]
            [ez-email.core :as email]))

(let [provider-settings {;; this is the default from
                         :from "test@example.com"
                         ;; this is data for postal
                         :postal {:user "test@example.com"
                                  :pass "my password"
                                  :ssl true
                                  :host "smtp.example.com"}}
      provider (email/get-provider provider-settings)]
  (println "Init done")
  (let [c (:c-result provider)]
    (async/go-loop []
      (when-let [v (async/<! c)]
        (println "Got the result from sending the message")
        (println v)
        (recur))))
  (email/queue-message "test@test-me.com" "This is a test from ez-email" "nt")
  (println "Sleeping...")
  (Thread/sleep 4000)
  (println "Shutting down core.async channel")
  (email/shutdown!))
```

## Error handling

The way ez-email is meant to work is to send the result of the sending of the message to a core async channel, which by default is a sliding-buffer of size 1. Listen in on the chafnnel and handle errors however you see fit. The internal for the channels names are c-in and c-result. See implementation of the PostalProvider for more details.

## License

Copyright © 2016 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

---

Coram Deo
