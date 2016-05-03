(ns server.service.github
  (:refer-clojure :exclude [reduce into])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [<! put! chan onto-chan reduce close!]]
            [common.util :refer [js-log log js->cljs]]
            [server.config :refer [config]]))

; channels
(defonce repo-ch (chan))
(defonce commit-ch (chan))
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
      (when (some? page)
        (onto-chan ch (js->clj page) false))
      (if (.hasNextPage client page)
        ; then
        (.getNextPage client page (paginate ch))
        ; else
        (close! ch))))


; (go-loop [repo (<! repo-ch)]
;   (if repo
;     (do
;       (let [out-ch (chan)
;             user   (-> repo (get "owner" ) (get "login"))
;             repo   (get repo "name")
;             author (-> config (.-github) (.-username))
;             since  (let [now (js/Date.)]
;                      (.setFullYear now (- (.getFullYear now) 1)))]
;         (print repo)
;         (. RepoApi (getCommits (clj->js {:user      user
;                                          :repo      repo
;                                          :author    author
;                                          "per_page" 100
;                                          :since     since})
;                                (paginate out-ch)))
;         (pipe out-ch commit-ch false))
;       (recur (<! repo-ch)))
;     (close! commit-ch)))

; (go-loop []
;   (let [repo   (<! repo-ch)
;         out-ch (chan)
;         user   (-> repo (get "owner" ) (get "login"))
;         repo   (get repo "name")
;         author (-> config (.-github) (.-username))
;         since  (let [now (js/Date.)]
;                  (.setFullYear now (- (.getFullYear now) 1)))]
;     (when-not repo
;       (print "done"))
;     (. RepoApi (getCommits (clj->js {:user      user
;                                      :repo      repo
;                                      :author    author
;                                      "per_page" 100
;                                      :since     since})
;                            (paginate out-ch)))
;     (pipe out-ch commit-ch false))
;   (recur))

(defn- reduce-repos [acc repo]
  (let [out-ch (chan)
        user   (-> repo (get "owner" ) (get "login"))
        repo   (get repo "name")
        author (-> config (.-github) (.-username))
        since  (let [now (js/Date.)]
                 (.setFullYear now (- (.getFullYear now) 1)))]
    (. RepoApi (getCommits (clj->js {:user      user
                                     :repo      repo
                                     :author    author
                                     "per_page" 100
                                     :since     since})
                           (paginate out-ch)))
    (<! (reduce (fn [acc commit]
                   (print commit)
                   acc)
                 acc
                 out-ch))))

(def commit-result (reduce reduce-repos
                           {}
                           repo-ch))

; @TODO: account for error in all requests
; @TODO: native clj transformations to transit
(defn get-activity
  []
  ; paginate through all user repos
  (.getAll RepoApi (clj->js {"per_page" 100}) (paginate repo-ch))
  ; reduce over commits and add them together
  ; (reduce #(inc %1) 0 commit-ch))
  commit-result)
