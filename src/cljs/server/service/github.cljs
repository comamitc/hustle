(ns server.service.github
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! put! chan onto-chan close!]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ctime]
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
        (let [p (js->clj page)]
          (log (str (count p) " " (.hasNextPage client page)))
          (onto-chan ch p false)))

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
  (let [date (-> commit
                 (get "commit")
                 (get "committer")
                 (get "date")
                 (ctime/to-local-date)
                 (ctime/to-string))]
    (merge-with + acc {date 1})))

; @TODO: account for error in all requests
; @TODO: native clj transformations to transit
(defn get-activity
  []
  ; paginate through all user repos
  (.getAll RepoApi (clj->js {"per_page" 100}) (paginate repo-ch))
  ; reduce over commits and add them together
  ;(reduce #(inc %1) 0 commit-ch))
  (go
    (let [chs           (<! (async/reduce reduce-repos [] repo-ch))
          repo-commits  (async/merge chs)]
      (<! (async/reduce reduce-commits {} repo-commits)))))
