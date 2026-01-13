(ns vm.test.vm
  (:require [vm]
            [clojure.string :as str]
            [vm.parser :as p]
            [vm.code-writer :as cw]
            [hack-interpreter :as itp]))

(def fn-call-test ["function Foo 0" "return" "function Sys.init 0" "call Foo 0" "label LOOP" "goto LOOP"])

(def asm (vm/vm-to-asm "Filename" fn-call-test))
(println (str/join "\n" asm))
(itp/interpret asm)
