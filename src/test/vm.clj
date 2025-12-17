(ns test.vm
  (:require [vm]
            [vm.parser :as p]
            [vm.code-writer :as cw]
            [hack-interpreter :as itp]))

(defn empty-test [] (vm/run "filename" "" 0 0 [] []))

