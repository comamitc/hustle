(ns server.core
  (:require [cljs.nodejs :as nodejs]
            [figwheel.client :as fw]
            [server.util :as util]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce body-parser (nodejs/require "body-parser"))

(def app (express))
(def port 3779)

;; attach body-parser
(.use app (.urlencoded body-parser #js {:extended false}))
(.use app (.json body-parser))

;; mount routes
(. app (get "/api/github" (fn [req res] (.send res "Hello, World!"))))
(. app (get "/api/bitbucket" (fn [req res] (.send res "Hello, World!"))))

(. app (use (serve-static "resources/public" #js {:index "index.html"})))

(def -main
  (fn []
      (doto (.createServer http #(app %1 %2))
            (.listen port))))

(set! *main-cli-fn* -main)

(when-not (= (util/env "NODE_ENV") "production")
  (fw/start {}))
