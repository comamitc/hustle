(ns server.service.github
  (:require [cljs.nodejs :as nodejs]
            [server.config :refer [config]]))

(defonce Github (nodejs/require "github"))
(defonce client (Github. #js {:version "3.0.0"
                              :host "api.github.com"
                              :debug true})) ;; @TODO: toggle based on NODE_ENV

;; authenticate client to github using basic credentials
(. client (authenticate (:github conf)))

(defn- get-repos
  []
  nil)

(defn- get-commits
  []
  nil)

;; public api
(defn get-activity
  []
  nil)
