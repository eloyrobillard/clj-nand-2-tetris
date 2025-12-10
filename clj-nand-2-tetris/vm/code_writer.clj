(ns code-writer
  (:require [clojure.string :as str])
  (:require [clojure.core.match :refer [match]]))

(defn get-value-with-offset [base offset]
  [base "D=M" (str/join ["@" offset]) "A=D+A"])

(defn get-address [filename segment offset]
  {:pre [(string? filename) (string? segment) (string? offset)]
   :post [(or (sequential? %) (string? %))]}
  (match segment
    "constant" (str/join ["@" offset])
    "static" (str/join ["@" filename "." offset])
    "local" (get-value-with-offset "@LCL" offset)
    "argument" (get-value-with-offset "@ARG" offset)
    "this" (get-value-with-offset "@THIS" offset)
    "that" (get-value-with-offset "@THAT" offset)
    "temp" (str/join ["@" (+ 5 (Integer/parseInt offset))])
    "pointer" (if
               (= offset "0")
                "@THIS"
                "@THAT")))

(def push-d ["@SP" "A=M" "M=D" "@SP" "M=M+1"])
(def pop-d ["@SP" "M=M-1" "A=M" "D=M"])

(defn push-constant [const]
  {:pre [(string? const)]}
  [(str "@" const) "D=A" push-d])

(defn pop-direct-address [address]
  (flatten [pop-d address "M=D"]))

(defn pop-temp [offset]
  {:pre [(int? offset)]}
  (flatten [pop-d (str "@" (+ offset 5)) "M=D"]))

(defn pop-indirect-address [address]
  (flatten [(pop-temp 0) address "D=A" "@6" "M=D" "@5" "D=M" "@6" "A=M" "M=D"]))

(defn write-push-pop [filename op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)
        address (get-address filename a1 a2)]
    (match type
      :c-push (flatten (match a1
                         "constant" (push-constant a2)
                         :else [address "D=M" push-d]))
      :c-pop (if (= a1 "temp")
               (pop-direct-address address)
               (pop-indirect-address address)))))

(defn setup-boolean-op [filename op]
  (let [suffix (str/upper-case (:a1 op))]
    ["D=D-M"
     (str/join ["@" filename "." suffix "." (:ln op)])
     (str/join ["D;J" suffix])
     "D=0"
     (str/join ["@N" filename "." suffix "." (:ln op)])
     "0;JMP"
     (str/join ["(" filename "." suffix "." (:ln op) ")"])
     "D=-1"
     (str/join ["(N" filename "." suffix "." (:ln op) ")"])]))

(defn arithm [filename op]
  (match (:a1 op)
    "eq" (setup-boolean-op filename op)
    "lt" (setup-boolean-op filename op)
    "gt" (setup-boolean-op filename op)
    "and" "D=D&M"
    "or" "D=D|M"
    "neg" "D=-D"
    "not" "D=!D"
    "add" "D=D+M"
    "sub" "D=D-M"))

(defn write-arithmetic [filename op]
  (let [a1 (:a1 op)]
    (if (or (= a1 "neg") (= a1 "not"))
      (flatten [pop-d (arithm filename op) push-d])
      (flatten [pop-d "@5" "M=D" pop-d "@5" (arithm filename op) push-d]))))

(defn write-label [filename fname op]
  (let [label (:a1 op)]
    [(str/join ["(" filename "." fname "$" label ")"])]))

(defn write-goto [filename fname op]
  (let [label (:a1 op)]
    [(str/join ["@" filename "." fname "$" label]) "0;JMP"]))

(defn write-if [filename fname op]
  (let [label (:a1 op)]
    (flatten [pop-d (str/join ["@" filename "." fname "$" label]) "D;JNE"])))

(defn write-function [op]
  {:pre [(some? (:a2 op))]}
  (flatten [(str "(" (:a1 op) ")") (repeat (Integer/parseInt (:a2 op)) (push-constant "0"))]))

(defn var-to-var [v1 v2]
  [(str "@" v1) "D=M" (str "@" v2) "M=D"])

(defn assign [r l]
  (flatten [l r "M=D"]))

(defn subtract-const [var const]
  (flatten [var const "D=D-A"]))

(defn add-const [var const]
  (flatten [var const "D=D+A"]))

(defn get-var [segment]
  [segment "D=M"])

(defn goto [dest]
  (flatten [dest "0;JMP"]))

(defn dref [ref]
  (flatten [ref "A=D" "D=M"]))

(defn write-return [filename]
  (flatten
   [; frame = LCL
    (assign "@retAddr" (dref (subtract-const (get-var "LCL") "5"))) ; retAddr = *(frame-5)
    (write-push-pop filename {:type :c-pop :a1 "argument" :a2 "0"}); *ARG = pop()
    (assign "@SP" (dref (add-const (get-var "ARG") "1"))) ; SP = ARG + 1
    (assign "@THAT" (dref (subtract-const (get-var "LCL") "1"))) ; THAT = *(frame-1)
    (assign "@THIS" (dref (subtract-const (get-var "LCL") "2"))) ; THIS = *(frame-2)
    (assign "@ARG" (dref (subtract-const (get-var "LCL") "3"))) ; ARG = *(frame-3)
    (assign "@LCL" (dref (subtract-const (get-var "LCL") "4"))) ; LCL = *(frame-4)
    (goto "@retAddr") ; goto retAddr
    ]))

(defn push-segment [segment] [(str "@" segment) "D=M" push-d])

(defn write-call [fname call-num op]
  (let [ret-addr (str fname "$ret." call-num)
        f (:a1 op)
        n-args (:a2 op)]
    (flatten
     [(str "@" ret-addr) "D=A" push-d ; push retAddr
      (push-segment "LCL") ; push LCL
      (push-segment "ARG") ; push ARG
      (push-segment "THIS") ; push THIS
      (push-segment "THAT") ; push THAT
      (var-to-var "ARG" "SP") (str "@" (+ 5 (Integer/parseInt n-args))) "D=A" "@ARG" "M=M-D" ; ARG = SP-5-nArgs
      (var-to-var "SP" "LCL") ; LCL = SP
      (str "@" f) "0;JMP" ; goto f
      (str "(" ret-addr ")") ; (retAdr)
      ])))

(defn write [filename fname call-num op]
  (let [type (:type op)]
    {:fname (if (= :c-function type) (:a1 op) fname)
     :call-num (if (= :c-call type) (+ call-num 1) call-num)
     :asm (match type
            :c-function (write-function op)
            :c-return (write-return filename)
            :c-call (write-call fname call-num op)
            :c-label (write-label filename fname op)
            :c-goto (write-goto filename fname op)
            :c-if (write-if filename fname op)
            :c-arithm (write-arithmetic filename op)
            :c-push (write-push-pop filename op)
            :c-pop (write-push-pop filename op))}))

