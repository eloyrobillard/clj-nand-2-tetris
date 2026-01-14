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

(defn run-a [sym sym-tbl state]
  {:pre [(string? sym)]}
  (let [new-state (if (Character/isDigit (first sym))
                    (assoc state :A (Integer/parseInt sym))
                    (assoc state :A (sym-tbl sym)))]
    (assoc new-state :M (get (:mem new-state) (:A new-state)))))

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
    "D+1" (+ (:D state) 1)
    "A+1" (+ (:A state) 1)
    "M+1" (+ (:M state) 1)
    "D-1" (- (:D state) 1)
    "A-1" (- (:A state) 1)
    "M-1" (- (:M state) 1)
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

(defn get-jmp-res [jmp cmp]
  {:pre [(or (nil? jmp) (string? jmp))] :post [(boolean? %)]}
  (match jmp
    nil false
    "JGT" (> cmp 0)
    "JEQ" (= cmp 0)
    "JGE" (>= cmp 0)
    "JLT" (< cmp 0)
    "JNE" (not (= cmp 0))
    "JLE" (<= cmp 0)
    "JMP" true))

(defn update-state [state keys val]
  (reduce #(assoc %1 %2 val) state keys))

(defn run-c [dest cmp jmp state]
  (let [d (get-dest dest)
        c (get-cmp-val cmp state)
        j (get-jmp-res jmp c)]
    (if (nil? d)
      (if (true? j)
        (assoc state :PC (:A state))
        state)
      (if (.contains d :M)
        (update-state (assoc state :mem (assoc (:mem state) (:A state) c)) d c)
        (update-state state d c)))))

(defn get-stack-reading [st state]
  (loop [i 257
         result (str (get (:mem state) 256))]
    (if (<= i (get (:mem state) (st/get-address st "SP")))
      (recur (+ i 1) (str result "," (get (:mem state) i)))
      result)))

(defn interpret-aux [seq st state]
  {:pre [(map? st)]}
  (if (constantly true)
    (let [pc (:PC state)
          instr (nth seq pc)
          type (p/instruction-type instr)
          new-state (assoc state :PC (+ 1 (:PC state)))]
      (println
       (format "P: %d,\tA: %d,\tD: %d,\tM: %d,\tStk[%d]: %d,\tRet: %d\t> %s,\t\t%s"
               (:PC state)
               (:A state)
               (:D state)
               (:M state)
               (nth (:mem state) (st/get-address st "SP"))
               (nth (:mem state) (nth (:mem state) (st/get-address st "SP")))
               (nth (:mem state) (st/get-address st "retAddr"))
               instr
               (get-stack-reading st state)))
      (interpret-aux
       seq
       st
       (if (= type :a-instr)
         (let [sym (p/sym instr :a-instr)]
           (run-a sym st new-state))
         (run-c (p/dest instr) (p/cmp instr) (p/jump instr) new-state))))
    state))

(def st {"R0" 0, "R1" 1, "R2" 2, "R3" 3, "R4" 4, "R5" 5, "R6" 6, "R7" 7, "R8" 8, "R9" 9, "R10" 10, "R11" 11, "R12" 12, "R13" 13, "R14" 14, "R15" 15, "SP" 0, "LCL" 1, "ARG" 2, "THIS" 3, "THAT" 4, "SCREEN" 16384, "KBD" 24576})

(defn interpret [seq]
  (let [sq (rm-non-code seq)
        sym-tbl (populate-symbol-table sq st)
        state {:A 0 :D 0 :M 0 :PC 0 :mem (into [] (repeat (+ (math/pow 2 24) 1) 0))}]
    (-> sq
        rm-l-instr
        (interpret-aux sym-tbl state))))

(defn -main []
  (let [file-input (with-open [rdr (clojure.java.io/reader (first *command-line-args*))]
                     (into [] (line-seq rdr)))]
    (spit "test.hack" (str/join "\n" (interpret file-input)))))
