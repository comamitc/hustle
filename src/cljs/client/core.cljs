(ns client.core
    (:require [client.components.activity.core :as activity]
              [client.config :as config]))

; @TODO - figure out difference between dev and prod for exporting
(defn ^:export init []
  (activity/render "app"))
