(ns hack-interpreter
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [clojure.math :as math])
  (:require [clojure.core.match :refer [match]])
  (:require [assembler.symbol-table :as st])
  (:require [assembler.parser :as p]))

(defn rm-non-code [seq]
  (filter #(not (or (p/is-ws? %) (p/is-cmt? %))) seq))

(defn rm-l-instr [seq]
  (filter #(not (= (p/instruction-type %) :l-instr)) seq))

; (rm-non-code '())
; (rm-non-code '("" "// sdf" "sdf" "// ewr" ""))
; (rm-non-code '("@456" "@123" "" "// wer" "@echo" "D=0" "0;JMP"))

(defn populate-st-l [st seq ln]
  {:pre [(map? st)]}
  (if-not (some? (first seq))
    st
    (let [head (first seq)
          type (p/instruction-type head)]
      (if (= :l-instr type)
        (let [sym (p/sym head :l-instr)]
          (if (st/contains st sym)
            (recur st (rest seq) ln)
            (recur (st/add-entry st sym ln) (rest seq) ln)))
        (recur st (rest seq) (+ ln 1))))))

(defn populate-st-a [st seq addr]
  {:pre [(>= addr 16) (< addr 16384) (map? st)]}
  (if-not (some? (first seq))
    st
    (let [head (first seq)
          type (p/instruction-type head)]
      (if (= :a-instr type)
        (let [sym (p/sym head :a-instr)]
          (if (or (st/contains st sym) (Character/isDigit (first sym)))
            (recur st (rest seq) addr)
            (recur (st/add-entry st sym addr) (rest seq) (+ addr 1))))
        (recur st (rest seq) addr)))))

(defn populate-symbol-table [seq st]
  {:pre [(map? st)]}
  (-> st
      (populate-st-l seq 0)
      (populate-st-a seq 16)))

(defn set-reg [reg val state]
  (assoc state reg val))

(defn run-a [sym sym-tbl state]
  {:pre [(string? sym)]}
  (if (Character/isDigit (first sym))
    (set-reg "A" (Integer/parseInt sym) state)
    (set-reg "A" (sym-tbl sym) state)))

(defn get-dest [in]
  (match in
    nil nil
    "M" [:M]
    "D" [:D]
    "DM" [:D :M]
    "MD" [:D :M]
    "A" [:A]
    "AM" [:A :M]
    "AD" [:A :D]
    "ADM" [:A :D :M]))

(defn get-cmp-val [in state]
  (match in
    "0" 0
    "1" 1
    "-1" -1
    "D" (:D state)
    "A" (:A state)
    "M" (:M state)
    "!D" (bit-not (:D state))
    "!A" (bit-not (:A state))
    "!M" (bit-not (:M state))
    "-D" (- (:D state))
    "-A" (- (:A state))
    "-M" (- (:M state))
    "D+1" (+ 1 (:D state))
    "A+1" (+ 1 (:A state))
    "M+1" (+ 1 (:M state))
    "D-1" (- 1 (:D state))
    "A-1" (- 1 (:A state))
    "M-1" (- 1 (:M state))
    "D+A" (+ (:D state) (:A state))
    "D+M" (+ (:D state) (:M state))
    "D-A" (- (:D state) (:A state))
    "D-M" (- (:D state) (:M state))
    "A-D" (- (:A state) (:D state))
    "M-D" (- (:M state) (:D state))
    "D&A" (bit-and (:D state) (:A state))
    "D&M" (bit-and (:D state) (:M state))
    "D|A" (bit-or (:D state) (:A state))
    "D|M" (bit-or (:D state) (:M state))))

(defn get-jmp-res [in cmp state]
  {:pre [(string? in)] :post [(boolean? %)]}
  (match in
    nil false
    "JGT" (> cmp 0)
    "JEQ" (= cmp 0)
    "JGE" (>= cmp 0)
    "JLT" (< cmp 0)
    "JNE" (not (= cmp 0))
    "JLE" (<= cmp 0)
    "JMP" true))

(defn run-c [dest cmp jmp state]
  (let [d (get-dest dest)
        c (get-cmp-val cmp state)
        j (get-jmp-res jmp c state)]
    (if (nil? d)
      (if (true? j)
        (assoc state :PC (:A state))
        state)
      (reduce (#(assoc %1 %2 c)) state d))))

(reduce #(assoc %1 %2 5) {:x 1 :y 2} [:y])

(defn interpret-aux [seq st state]
  {:pre [(map? st)]
   :post [(every? string? %)]}
  (let [pc (:PC state)
        instr (get seq pc)
        type (p/instruction-type instr)]
    (if (= type :a-instr)
      (let [sym (p/sym instr :a-instr)]
        (run-a sym st state))
      (run-c (p/dest instr) (p/cmp instr) (p/jump instr) state))
    seq))

(def st {"R0" 0, "R1" 1, "R2" 2, "R3" 3, "R4" 4, "R5" 5, "R6" 6, "R7" 7, "R8" 8, "R9" 9, "R10" 10, "R11" 11, "R12" 12, "R13" 13, "R14" 14, "R15" 15, "SP" 0, "LCL" 1, "ARG" 2, "THIS" 3, "THAT" 4, "SCREEN" 16384, "KBD" 24576})

(defn interpret [seq]
  (let [sq (rm-non-code seq)
        sym-tbl (populate-symbol-table sq st)
        state {:A 0 :D 0 :M 0 :PC 0 :mem (take (+ (math/pow 2 24) 1))}]
    (-> sq
        rm-l-instr
        (interpret-aux sym-tbl state))))

(defn -main []
  (let [file-input (with-open [rdr (clojure.java.io/reader (first *command-line-args*))]
                     (into [] (line-seq rdr)))]
    (spit "test.hack" (str/join "\n" (interpret file-input)))))
