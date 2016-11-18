(ns request.test.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [request.core-test]
            [request.middleware-test]
            [request.util-test]))

(doo-tests
 'request.core-test
 'request.middleware-test
 'request.util-test)
