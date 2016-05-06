(ns client.core
    (:require [client.components.activity.core :as activity]
              [client.config :as config]))

(defn ^:export activityGraph [elm-id]
  (activity/render elm-id))
