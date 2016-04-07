(ns syng-im.handlers.sign-up
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [syng-im.db :as db]
            [syng-im.models.commands :as commands]
            [syng-im.utils.utils :refer [log on-error http-post toast]]
            [syng-im.utils.logging :as log]
            [syng-im.utils.random :as random]
            [syng-im.utils.phone-number :refer [format-phone-number]]
            [syng-im.constants :refer [text-content-type
                                       content-type-command
                                       content-type-command-request]]))

(defn intro [db]
  (dispatch [:received-msg
             {:msg-id "1"
              :content "Hello there! It's Syng, a Dapp browser in your phone."
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  (dispatch [:received-msg
             {:msg-id "2"
              :content (str "Syng uses  a highly secure key-pair authentication type "
                            "to provide you a reliable way to access your account")
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  (dispatch [:received-msg
             {:msg-id "3"
              :content (commands/format-command-request-msg-content
                        :keypair-password
                        (str "A key pair has been generated and saved to your device. "
                             "Create a password to secure your key"))
              :content-type content-type-command-request
              :outgoing false
              :from "console"
              :to "me"}])
  ;; (dispatch [:set-chat-command :keypair-password])
  db)

(defn send-console-msg [text]
  {:msg-id       (random/id)
   :from         "me"
   :to           "console"
   :content      text
   :content-type text-content-type
   :outgoing     true})

(defn- handle-password [content]
  ;; TODO validate and save password
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content (str "OK great! Your password has been saved. Just to let you "
                            "know, you can always change it in the Console, by the way, "
                            "it's me, the Console, nice to meet you!")
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content (str "I'll generate a passphrase for you so you can restore your "
                            "access or log in from another device")
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content "Here's your passphrase:"
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  ;; TODO generate passphrase
  (let [passphrase (str "The brash businessman's braggadocio and public squabbing with "
                        "candidates in the US presidential election")]
    (dispatch [:received-msg
               {:msg-id (random/id)
                :content passphrase
                :content-type text-content-type
                :outgoing false
                :from "console"
                :to "me"}]))
  (dispatch [:received-msg
             {:msg-id "8"
              :content "Make sure you had securely written it down"
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}])
  ;; TODO highlight '!phone'
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content (commands/format-command-request-msg-content
                        :phone
                        (str "Your phone number is also required to use the app. Type the "
                             "exclamation mark or hit the icon to open the command list "
                             "and choose the !phone command")                        )
              :content-type content-type-command-request
              :outgoing false
              :from "console"
              :to "me"}]))


;; -- Send phone number ----------------------------------------
(defn on-sign-up-response []
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content (commands/format-command-request-msg-content
                        :confirmation-code
                        (str "Thanks! We've sent you a text message with a confirmation "
                             "code. Please provide that code to confirm your phone number"))
              :content-type content-type-command-request
              :outgoing false
              :from "console"
              :to "me"}]))

(defn- handle-phone [content]
  (let [phone-number (format-phone-number content)]
    (dispatch [:sign-up phone-number on-sign-up-response])))


;; -- Phone number confirmation --------------------------------
(defn on-sync-contacts []
  (dispatch [:received-msg
             {:msg-id (random/id)
              :content (str "Your contacts have been synchronized")
              :content-type text-content-type
              :outgoing false
              :from "console"
              :to "me"}]))

(defn sync-contacts []
  (dispatch [:sync-contacts on-sync-contacts]))

(defn on-send-code-response [body]
  (if (:confirmed body)
    (do (dispatch [:received-msg
                   {:msg-id (random/id)
                    :content "Confirmed"
                    :content-type text-content-type
                    :outgoing false
                    :from "console"
                    :to "me"}])
        (sync-contacts))
    (dispatch [:received-msg
               {:msg-id (random/id)
                :content "Wrong code"
                :content-type text-content-type
                :outgoing false
                :from "console"
                :to "me"}])))

(defn send-code [code]
  (dispatch [:sign-up-confirm code on-send-code-response]))

(defn- handle-confirmation-code [content]
  (send-code content))

;; TODO store command key in a separate field
(defn send-console-command [command content]
  (when (= command :keypair-password)
    (handle-password content))
  (when (= command :phone)
    (handle-phone content))
  (when (= command :confirmation-code)
    (handle-confirmation-code content))
  {:msg-id       (random/id)
   :from         "me"
   :to           "console"
   :content      (commands/format-command-msg-content command content)
   :content-type content-type-command
   :outgoing     true})