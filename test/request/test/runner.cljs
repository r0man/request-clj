(ns request.test.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [request.core-test]))

(doo-tests 'request.core-test)
