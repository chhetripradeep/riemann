(ns riemann.rocket
  "Forwards riemann events to Rocket Chat."
  (:require [clojure.string :as str]
            [clj-http.client :as http])
  (:use [clojure.string :only [escape]]))

(def ^:private rocket-url
  "%s://%s:%s/api/v1/chat.postMessage")

(defn rocket-escape
  "Escape message according to rocket formatting spec."
  [message]
  (escape message {\< "&lt;" \> "&gt;" \& "&amp;"}))

(defn generate-body
  "Prepares the body of the rocket message."
  [opts event]
  {:channel (:channel opts)
   :text "Event Details",
   :attachments
   [{:text (rocket-escape (or (:description event) "")),
     :collapsed false,
     :color "#ff0000",
     :fields
     [{:title "Host",
       :value (rocket-escape (or (:host event) "-")),
       :short true}
      {:title "Service",
       :value (rocket-escape (or (:service event) "-")),
       :short true}
      {:title "Metric",
       :value (or (:metric event) "-"),
       :short true}
      {:title "State",
       :value (rocket-escape (or (:state event) "-")),
       :short true}
      {:title "Description",
       :value (rocket-escape (or (:description event) "-"))
       :short true}
      {:title "Tags",
       :value (rocket-escape (str (into [] (:tags event))))
       :short true}]}]})

(defn post-message
  "Post the message as rocket message."
  [opts event]
  (let [url (format rocket-url (:scheme opts) (:host opts) (:port opts))
        http-options {:body (generate-body opts event)
                      :content-type :json
                      :conn-timeout 5000
                      :socket-timeout 5000
                      :throw-entire-message? true
                      :headers { "X-User-Id" (:userid opts)
                                 "X-Auth-Token" (:authtoken opts) }}]
    (http/post url http-options)))

(defn rocket
  "Returns a function which accepts an event and sends it to rocket.

   Usage:
   (prometheus {:host \"riemann-test.rocket.chat\"})

   Options:
   `:scheme`         Uri scheme (default: \"https\")
   `:host`           Host running rocket chatserver (default: \"localhost\")
   `:port`           Port used by rocket chat (default: 443)
   `:channel`        Rocket Channel to post messages (default: \"#general\")
   `:authtoken`      Auth token used to post messages
   `:userid`         User Id to post messages as
  "
  [opts]
  (let [opts (merge {:scheme    "https"
                     :host      "localhost"
                     :port      443
                     :channel   "#general"}
                    opts)]
    (fn [event]
      (post-message opts event))))
