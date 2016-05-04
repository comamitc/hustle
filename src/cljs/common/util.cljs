(ns common.util
  (:require [cognitect.transit :as t]))

(defn log
  "Log a Clojure thing."
  [thing]
  (js/console.log (pr-str thing)))

(defn js-log
  "Log a JavaScript thing."
  [thing]
  (js/console.log thing))

;; @TODO: standardize to stringify keywords.
(defn cljs->js
  "takes native cljs data structure and converts to json"
  [x]
  (let [w (t/writer :json-verbose)]
    (t/write w x)))

(defn js->cljs [x]
  (let [r   (t/reader :json)
        str (if-not (string? x) (.stringify js/JSON x) x)]
    (t/read r str)))
