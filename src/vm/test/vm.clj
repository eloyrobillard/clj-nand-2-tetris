(ns vm.test.vm
  (:require [vm]
            [vm.parser :as p]
            [vm.code-writer :as cw]
            [hack-interpreter :as itp]))

; returns []
; (vm/run "src/vm/test/SimpleAdd.vm" "" 0 0 [] [])

(def in (vm/vm-to-asm "src/vm/test/SimpleAdd.vm"))

(itp/rm-non-code in)
(itp/interpret in)

