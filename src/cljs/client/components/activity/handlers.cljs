(ns client.components.activity.handlers
  (:require [re-frame.core :as re-frame]
            [client.components.activity.db :as db]))

(re-frame/register-handler
  :initialize-db
  (fn  [_ _]
    db/default-db))
