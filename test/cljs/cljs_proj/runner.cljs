(ns cljs-proj.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [cljs-proj.core-test]))

(doo-tests 'cljs-proj.core-test)
