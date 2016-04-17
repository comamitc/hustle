(ns server.util
  (:require [cljs.nodejs :as nodejs]))

(defn env
  "Returns the value of the environment variable k"
  [k]
  (let [e (js->clj (.-env nodejs/process))]
    (print (get e k))
    (get e k)))
