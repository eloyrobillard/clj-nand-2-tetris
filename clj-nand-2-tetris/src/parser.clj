(ns parser
  (:require [clojure.string :as str]))

(defn has-more-lines [seq] (some? (first (rest seq))))

(defn is-a? [instr] (str/starts-with? (str/triml instr) "@"))
(defn is-l? [instr]
  (str/starts-with?
   (str/triml instr) "("))

(defn is-cmt? [instr] (str/starts-with? (str/triml instr) "//"))
(defn is-ws? [instr] (empty? (str/triml instr)))

(defn skip-non-code [seq]
  (loop [sq seq] (if-not (and
                          (has-more-lines sq)
                          (or
                           (is-ws? (first sq))
                           (is-cmt? (first sq))))
                   sq
                   (recur (rest sq)))))

; (has-more-lines '("" "// sdf"))
; (is-cmt? "  // sdf")
; (is-ws? "")
; (is-cmt? "  // sdf")
; (skip-non-code '("" "// sdf" "sdfsdf" "// sdf"))
; (skip-non-code '("// sdf" "" "sdfsdf" "// sdf"))
; (skip-non-code '("// sdf" "sdf"))
; (skip-non-code '("sdf" "// sdf"))
; (skip-non-code '(""))
; (skip-non-code '())

(defn advance [seq]
  (let [sq (skip-non-code seq)]
    (if (some? (first sq))
      (rest sq)
      sq)))

; (advance '("" "// sdf" "sdfsdf" "// sdf"))
; (advance '("// sdf" "" "sdfsdf" "// sdf"))
; (advance '("// sdf" "sdf"))
; (advance '("sdf" "// sdf"))
; (advance '(""))
; (advance '())

(defn instruction-type [instr]
  (cond
    (is-a? instr) :a-instr
    (is-l? instr) :l-instr
    :else :c-instr))

(defn sym [type instr]
  (cond
    (= type :a-instr) (str/replace instr "@" "")
    (= type :l-instr) (str/replace instr #"[()]" "")
    :else instr))

; (sym :a-instr "@123")
; (sym :a-instr "@wer")
; (sym :l-instr "(LOOP)")

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
