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

(defn- next-page [curr-page]
  (let [out (chan)]
    (.getNextPage client curr-page
                         (fn [err next]
                           (put! out {:err err :page next} #(close! out))))
    out))

(defn- paginate
  [ch]
  (fn [err page]
    (let [p-ch (chan)]
      (put! p-ch {:err err :page page})
      (go-loop [result p-ch]
        (let [page  (:page (<! result))
              p (js->cljs (or page nil))]
          (when (< 0 (count p))
            (onto-chan ch p false))
          (if (.hasNextPage client page)
            ; then
            (recur (next-page page))
            ; else
            (close! ch)))))))

(defn- reduce-repos [acc repo]
  (let [out-ch (chan)
        user   (get-in repo ["owner" "login"])
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
                (get-in ["commit" "committer" "date"])
                (js/Date.)
                (.toLocaleDateString))]
    (merge-with + acc {date 1})))

; @TODO: account for error in all requests
; @TODO: native clj transformations to transit
(defn get-activity
  []
  (go
    ; paginate through all user repos
    (.getAll RepoApi #js {"per_page" 100}
                     (paginate repo-ch))
    ; reduce over commits and add them together
    (->> repo-ch
         (async/reduce reduce-repos [])
         (<!)
         (async/merge)
         (async/reduce reduce-commits {})
         (<!))))
