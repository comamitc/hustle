(ns hustle.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [hustle.core-test]))

(doo-tests 'hustle.core-test)
