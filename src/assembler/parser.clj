(ns assembler.parser
  (:require [clojure.string :as str]))

(defn is-a? [instr]
  {:pre [(some? instr)]}
  (str/starts-with? (str/triml instr) "@"))

(defn is-l? [instr]
  (str/starts-with?
   (str/triml instr) "("))

(defn is-cmt? [instr] (str/starts-with? (str/triml instr) "//"))
(defn is-ws? [instr] (empty? (str/triml instr)))

; (has-more-lines '("" "// sdf"))
; (is-cmt? "  // sdf")
; (is-ws? "")
; (is-cmt? "  // sdf")

(defn instruction-type [instr]
  {:pre [(some? instr)]}
  (cond
    (is-a? instr) :a-instr
    (is-l? instr) :l-instr
    :else :c-instr))

(defn sym [instr type]
  {:pre [(or (= type :a-instr) (= type :l-instr))]
   :post [(some? %)]}
  (if (= type :a-instr)
    (str/replace (str/trim instr) "@" "")
    (str/replace (str/trim instr) #"[()]" "")))

; (sym "@123" {})
; (sym "@THIS" {"THIS" 3})
; (st/add-entry {} "NIL" 100)
; (sym "@NIL" {})

(defn dest [instr]
  (let [[_ match] (re-find #"(\w+)=" instr)]
    match))

; (dest "0;JMP")
; (dest "ADM=M-1")

(defn cmp [instr]
  (let [[_ m1 m2] (re-find #"(?:=(.+))|(?:\s*(.+);)" instr)]
    (or m1 m2)))

; (cmp "0;JMP")
; (cmp "ADM=M-1")

(defn jump [instr]
  (let [[_ match] (re-find #";(\w+)" instr)]
    match))

; (jump "0;JMP")
; (jump "ADM=M-1")
