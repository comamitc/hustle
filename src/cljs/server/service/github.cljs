(ns server.service.github
  (:refer-clojure :exclude [reduce into])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! put! chan onto-chan close!]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ctime]
            [cljs-time.format :as ftime]
            [common.util :refer [js-log log js->cljs cljs->js]]
            [server.config :refer [config]]))

; GitHub API
(def Github (nodejs/require "github"))
(def client (Github. #js {:version "3.0.0"
                          :host    "api.github.com"
                          :debug   false})) ; @TODO: toggle based on NODE_ENV

; authenticate client to github using basic credentials
(.authenticate client (.-github config))

; Global RepoApi for use
(def RepoApi (.-repos client))

; time span to capture
(def start (ctime/to-string (time/ago (time/years 1))))

(defn- paginate [ch]
  (fn [err page]
    (let [cntr (atom 0)]
      ; go-loop for recursion with channel to async calls
      (go-loop [result page
                acc    (js->cljs (or result nil))]
        (let [next? (.hasNextPage client result)]

          ; increment if next? else copy to channel and close
          (if next?
            (swap! cntr inc)
            (onto-chan ch acc))

          ; when next? async call and return that channel's result thru recur
          (when next?
            (let [next-page (<! (let [out (chan)]
                                  (.getNextPage
                                    client
                                    result
                                    (fn [err res]
                                        (put! out res #(do
                                                        (close! out)
                                                        (swap! cntr dec)))))
                                  out))]
              (recur next-page (concat acc
                                       (js->cljs next-page))))))))))

(defn- reduce-repos [acc repo]
  (let [out-ch (chan)
        user   (get-in repo ["owner" "login"])
        repo   (get repo "name")
        author (-> config (.-github) (.-username))]
    (. RepoApi (getCommits (cljs->js {"user"     user
                                      "repo"     repo
                                      "author"   author
                                      "per_page" 100
                                      "since"    start})
                           (paginate out-ch)))
    (conj acc out-ch)))

(defn- reduce-commits [acc commit]
  (let [date (-> commit
                (get-in ["commit" "committer" "date"])
                (js/Date.)
                (.toLocaleDateString))]
    (merge-with + acc {date 1})))

; @TODO: account for error in all requests
(defn get-activity
  []
  (let [repo-ch (chan)]
    (go
      ; paginate through all user repos
      (.getAll RepoApi (cljs->js {"per_page" 100})
                       (paginate repo-ch))
      ; reduce over commits and add them together
      (->> repo-ch
           (async/reduce reduce-repos [])
           (<!)
           (async/merge)
           (async/reduce reduce-commits {})
           (<!)))))
