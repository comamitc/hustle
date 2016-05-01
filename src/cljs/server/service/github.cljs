(ns server.service.github
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [<! put! chan onto-chan]]
            [common.util :refer [js-log log js->cljs]]
            [server.config :refer [config]]))

(defonce page-ch (chan))
(defonce repo-ch (chan))
(defonce Github (nodejs/require "github"))
(defonce client (Github. #js {:version "3.0.0"
                              :host    "api.github.com"
                              :debug   true})) ; @TODO: toggle based on NODE_ENV

;; authenticate client to github using basic credentials
(.authenticate client (.-github config))

; get commits
(go-loop []
  (let [repo (<! repo-ch)
        id   (get repo "id")
        u    (-> repo (get "owner") (get "login"))
        name (get repo "name")]
    (js-log (str "id: " id " name: " name " user: " u)))
  (recur))

; page channel
(go-loop []
  (let [page (<! page-ch)]
    ; exhaustively go through pages
    (when (.hasNextPage client page)
      (.getNextPage client page #(put! page-ch %2)))
    (let [p (js->clj page)]
      (log (first p))
      (onto-chan repo-ch p false)))
  (recur))

(defn get-activity
  []
  (let [RepoApi (.-repos client)]
    (.getAll RepoApi #js {"per_page" 100}
                     #(put! page-ch %2)))) ; @TODO: account for error
