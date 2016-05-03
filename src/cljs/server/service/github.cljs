(ns server.service.github
  (:refer-clojure :exclude [reduce into])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! put! chan onto-chan close!]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ctime]
            [cljs-time.format :as ftime]
            [common.util :refer [js-log log js->cljs]]
            [server.config :refer [config]]))

; channels
(defonce repo-ch (chan))

; GitHub API
(defonce Github (nodejs/require "github"))
(defonce client (Github. #js {:version "3.0.0"
                              :host    "api.github.com"
                              :debug   false})) ; @TODO: toggle based on NODE_ENV

; authenticate client to github using basic credentials
(.authenticate client (.-github config))

(defonce RepoApi (.-repos client))

(defn- paginate
  [ch]
  (fn [err page]
      (when page
        (onto-chan ch (js->clj page) false))

      (if (.hasNextPage client page)
        ; then
        (.getNextPage client page (paginate ch))
        ; else
        (close! ch))))

(defn- reduce-repos [acc repo]
  (let [out-ch (chan)
        user   (-> repo (get "owner" ) (get "login"))
        repo   (get repo "name")
        author (-> config (.-github) (.-username))
        since  (time/ago (time/years 1))]
    (. RepoApi (getCommits (clj->js {:user      user
                                     :repo      repo
                                     :author    author
                                     "per_page" 100
                                     :since     (ctime/to-string since)})
                           (paginate out-ch)))
    (conj acc out-ch)))

(defn- reduce-commits [acc commit]
  (let [formatter (ftime/formatter "yyyyMMdd")
        date      (-> commit
                      (get "commit")
                      (get "committer")
                      (get "date")
                      (ctime/from-string))]
    (merge-with + acc {(ftime/unparse formatter date) 1})))

; @TODO: account for error in all requests
; @TODO: native clj transformations to transit
(defn get-activity
  []
  ; paginate through all user repos
  (.getAll RepoApi (clj->js {"per_page" 100}) (paginate repo-ch))
  ; reduce over commits and add them together
  (go
    (->> repo-ch
         (async/reduce reduce-repos [])
         (<!)
         (async/merge)
         (async/reduce reduce-commits {})
         (<!))))
