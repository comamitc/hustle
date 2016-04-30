(ns server.interface.github
  (:require [cljs.nodejs :as node]))

(defonce Github (node/require "github"))
(defonce client (Github. #js {})
  :version "3.0.0"
  :host "api.github.com"
  :debug true) ;; @TODO: toggle based on NODE_ENV

;; public api
(defn get-activity
  []
  nil)
