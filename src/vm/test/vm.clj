(ns vm.test.vm
  (:require [vm]
            [vm.parser :as p]
            [vm.code-writer :as cw]
            [hack-interpreter :as itp]))

(def fn-call-test ["function Foo 0" "call Bar 0" "return" "function Bar 0" "push constant 85" "return" "function Sys.init 0" "call Foo 0" "label LOOP" "goto LOOP"])

(itp/interpret (vm/vm-to-asm "Filename" fn-call-test))
