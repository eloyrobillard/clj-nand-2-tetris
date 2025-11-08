(ns code-writer
  (:require [clojure.string :as str])
  (:require [clojure.core.match :refer [match]]))

(defn get-value-with-offset [base offset]
  [base "D=M" (str/join "" ["@" offset]) "A=D+A"])

(defn get-address [filename segment offset]
  {:pre [(string? filename) (string? segment) (string? offset)]
   :post [(sequential? %)]}
  (match segment
    "constant" [(str/join "" ["@" offset])]
    "static" [(str/join "." [(str/join "" ["@" filename]) offset]) "A=M"]
    "local" (get-value-with-offset "@LCL" offset)
    "argument" (get-value-with-offset "@ARG" offset)
    "this" (get-value-with-offset "@THIS" offset)
    "that" (get-value-with-offset "@THAT" offset)
    "temp" (get-value-with-offset "@TEMP" offset)
    "pointer" (if
               (= offset 0)
                (get-value-with-offset "@THIS" offset)
                (get-value-with-offset "@THAT" offset))))

; (get-address "Foo" "static" "8")
; (get-address "Foo" "constant" "8")
; (get-address "Foo" "local" "8")

(def push-to-stack ["@SP" "A=M" "M=D" "@SP" "M=M+1"])
(def popd ["@SP" "M=M-1" "A=M" "D=M"])
(defn write-push-pop [filename op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)
        address (get-address filename a1 a2)]
    (if (= type :c-push)
      (flatten (match a1
                 "constant" [address "D=A" push-to-stack]
                 :else [address "D=M" push-to-stack]))
      (flatten [popd "@TEMP" "M=D" address "M=D"]))))

(defn arithm [op]
  (match op
    "neg" "-D"
    "not" "!D"
    "add" "D+M"
    "sub" "D-M"))

(defn write-arithmetic [op]
  (let [a1 (:a1 op)]
    (if (or (= a1 "neg") (= a1 "not"))
      (flatten [popd (str/join "" ["D=" (arithm a1)]) push-to-stack])
      (flatten [popd "@TEMP" "M=D" popd "@TEMP" (str/join "" ["D=" (arithm a1)]) push-to-stack]))))

(defn write [filename op]
  (let [type (:type op)]
    (match type
      :c-arithm (write-arithmetic op)
      :else (write-push-pop filename op))))

