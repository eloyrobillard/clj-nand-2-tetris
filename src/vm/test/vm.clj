(ns test.vm
  (:require [vm]
            [vm.parser :as p]
            [vm.code-writer :as cw]
            [hack-interpreter :as itp]))

(vm/run "filename" "" 0 0 [] [])

