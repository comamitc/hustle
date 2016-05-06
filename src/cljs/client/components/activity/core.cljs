(ns client.components.activity.core
  (:require [cljsjs.d3]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [client.components.activity.handlers]
            [client.components.activity.subs]
            [cljs-time.core :as ct]
            [cljs-time.periodic :refer [periodic-seq]]
            [common.util :refer [log js-log]]))

;; todo - remove and add as parameters to the component
(def width 900)
(def height 400)

(defn- compute-date [week day]
  (let [dow (- 7 (ct/day-of-week (ct/now)))]
    (-> (ct/ago (ct/weeks (- 52 week)))
        (ct/minus (ct/days (-  (- 6 day) dow))))))

(defn- cal-grid []
  (let [weeks 53
        days  7
        edge  (/ width weeks)]
   (reduce (fn [acc day]
            (let [y (* day edge)]
              (conj acc
                    (reduce (fn [acc week]
                              (let [x (* week edge)]
                               (conj acc {:x x
                                          :y y
                                          :edge edge
                                          :date (compute-date week day)})))
                            []
                            (range weeks)))))
           []
           (range days))))

(def data (cal-grid))


(defn- graph [data]
  (reagent/create-class
    {:reagent-render      (fn [] [:div [:svg {:width width :height height}]])
     :component-did-mount (fn []
                              (let [d3data (clj->js data)
                                    row    (.. js/d3
                                               (select "svg")
                                               (selectAll ".row")
                                               (data d3data)
                                               enter
                                               (append "svg:g")
                                               (attr "class" "row"))

                                    div     (.. js/d3
                                                (select "body")
                                                (append "div")
                                                (attr "class" "tooltip")
                                                (style "opacity" 0))]

                                  (.. row
                                      (selectAll ".cell")
                                      (data (fn [d] d))
                                      enter
                                      (append "svg:rect")
                                      (attr "class" "cell")
                                      (attr "x" (fn [d] (.-x d)))
                                      (attr "y" (fn [d] (.-y d)))
                                      (attr "width" #(.-edge %))
                                      (attr "height" #(.-edge %))
                                      (attr "fill" "#fff")
                                      (attr "stroke" (fn [d]
                                                       (when (ct/within?
                                                               (ct/ago
                                                                 (ct/years 1))
                                                               (ct/now)
                                                               (.-date d))
                                                         "#ddd")))
                                      (on "mouseover"
                                          (fn [d]
                                            (.. div
                                                transition
                                                (duration 50)
                                                (style "opacity" 0.9))
                                            (.. div
                                                (html (.-date d)))))
                                      (on "mouseout"
                                          (fn [d]
                                            (.. div
                                                transition
                                                (duration 50)
                                                (style "opacity" 0)))))))}))
      ; :component-did-update (fn [this]
      ;                          (let [[_ data] (reagent/argv this)
      ;                                d3data (clj->js data)]
      ;                            (.. js/d3
      ;                                (selectAll ".row")
      ;                                (data d3data)
      ;                                (attr "class" "cell")
      ;                                (attr "x" (fn [d] (.-x d)))
      ;                                (attr "y" (fn [d] (.-y d)))
      ;                                (attr "width" (fn [d] (.-edge d)))
      ;                                (attr "height" (fn [d] (.-edge d)))
      ;                                (attr "fill" "#fff")
      ;                                (attr "stroke" "#ddd"))))}))


(defn- wrapper []
  [:div [graph data]])

(defn render [elm-id]
  (re-frame/dispatch-sync :initialize-db)
  (reagent/render [wrapper]
                  (.getElementById js/document elm-id)))
