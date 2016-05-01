(ns server.config
  (:require [cljs.nodejs :as nodejs]))

;; keep config in javascript since we are mostly passing values to other JS libs
(def config (nodejs/require "../../resources/config/application.json"))
