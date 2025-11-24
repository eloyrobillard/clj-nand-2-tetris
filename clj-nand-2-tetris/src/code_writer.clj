(ns code-writer
  (:require [clojure.string :as str])
  (:require [clojure.core.match :refer [match]]))

(defn get-value-with-offset [base offset]
  [base "D=M" (str/join "" ["@" offset]) "A=D+A"])

(defn get-address [filename segment offset]
  {:pre [(string? filename) (string? segment) (string? offset)]
   :post [(or (sequential? %) (string? %))]}
  (match segment
    "constant" (str/join "" ["@" offset])
    "static" (str/join "" ["@" filename "." offset])
    "local" (get-value-with-offset "@LCL" offset)
    "argument" (get-value-with-offset "@ARG" offset)
    "this" (get-value-with-offset "@THIS" offset)
    "that" (get-value-with-offset "@THAT" offset)
    "temp" (str/join "" ["@" (+ 5 (Integer/parseInt offset))])
    "pointer" (if
               (= offset "0")
                "@THIS"
                "@THAT")))

(def push-to-stack ["@SP" "A=M" "M=D" "@SP" "M=M+1"])
(def popd ["@SP" "M=M-1" "A=M" "D=M"])
(defn write-push-pop [filename op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)
        address (get-address filename a1 a2)]
    (match type
      :c-push (flatten (match a1
                         "constant" [address "D=A" push-to-stack]
                         :else [address "D=M" push-to-stack]))
      :c-pop (if (= a1 "temp")
               (flatten [popd address "M=D"])
               (flatten [popd "@5" "M=D" address "D=A" "@6" "M=D" "@5" "D=M" "@6" "A=M" "M=D"])))))

(defn setup-boolean-op [filename op]
  (let [suffix (str/upper-case (:a1 op))]
    ["D=D-M"
     (str/join "" ["@" filename "." suffix "." (:ln op)])
     (str/join "" ["D;J" suffix])
     "D=0"
     (str/join "" ["@N" filename "." suffix "." (:ln op)])
     "0;JMP"
     (str/join "" ["(" filename "." suffix "." (:ln op) ")"])
     "D=-1"
     (str/join "" ["(N" filename "." suffix "." (:ln op) ")"])]))

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
      (flatten [popd (arithm filename op) push-to-stack])
      (flatten [popd "@5" "M=D" popd "@5" (arithm filename op) push-to-stack]))))

(defn write-label [op]
  (let [label (:a1 op)]
    [(str/join "" ["(" label ")"])]))

(defn write-goto [op]
  (let [label (:a1 op)]
    [(str/join "" ["@" label]) "0;JMP"]))

(defn write-if [op]
  (let [label (:a1 op)]
    (flatten [popd (str/join "" ["@" label]) "D;JNE"])))

(defn write [filn op]
  (let [type (:type op)
        filename (str/replace filn #"\..*" "")]
    (match type
      :c-label (write-label op)
      :c-goto (write-goto op)
      :c-if (write-if op)
      :c-arithm (write-arithmetic filename op)
      :c-push (write-push-pop filename op)
      :c-pop (write-push-pop filename op))))

