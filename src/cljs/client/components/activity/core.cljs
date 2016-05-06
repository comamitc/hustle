(ns client.components.activity.core
  (:require [reagent.core :as reagent]
            [client.components.activity.handlers]
            [client.components.activity.subs]
            [re-frame.core :as re-frame]))

(defn render [elm-id]
  (re-frame/dispatch-sync :initialize-db)
  (reagent/render [:div "activity"]
                  (.getElementById js/document elm-id)))
