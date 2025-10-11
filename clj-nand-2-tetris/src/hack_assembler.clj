(ns hack-assembler
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [symbol-table :as st])
  (:require [code :as c])
  (:require [parser :as p]))

(defn rm-non-code [seq]
  (filter #(not (or (p/is-ws? %) (p/is-cmt? %))) seq))

(defn rm-l-instr [seq]
  (filter #(not (= (p/instruction-type %) :l-instr)) seq))

; (rm-non-code '())
; (rm-non-code '("" "// sdf" "sdf" "// ewr" ""))
; (rm-non-code '("@456" "@123" "" "// wer" "@echo" "D=0" "0;JMP"))

(defn c-to-bits [c-instr]
  (let [dest (p/dest c-instr)
        comp (p/cmp c-instr)
        jmp (p/jump c-instr)]
    (str/join "" ["111" (c/cmp comp) (c/dest dest) (c/jump jmp)])))

(defn populate-st-aux [seq ln st]
  {:pre [(map? st)]}
  (if-not (some? (first seq))
    st
    (let [head (first seq)]
      (if (= :l-instr (p/instruction-type head))
        (let [sym (p/sym head :l-instr)]
          (if (st/contains st sym)
            (recur (rest seq) ln st)
            (recur (rest seq) ln (st/add-entry st sym ln))))
        (recur (rest seq) (+ ln 1) st)))))

(defn populate-symbol-table [seq st]
  (populate-st-aux seq 0 st))

(defn str-to-bin [str]
  (-> str
      Integer/parseInt
      (Integer/toString 2)
      Integer/parseInt))

; (instr-to-bin "@128")
; (format "%016d" (instr-to-bin "@128"))

(defn sym-to-bin [st sym]
  {:pre [(map? st) (not (Character/isDigit (first sym)))]
   :post [(int? %)]}
  (-> sym
      (#(st/get-address st %))
      (Integer/toString 2)
      Integer/parseInt))

(defn assemble-aux [seq st]
  {:pre [(map? st)]
   :post [(every? string? %)]}
  (map (fn [instr]
         (let [type (p/instruction-type instr)]
           (if (= type :a-instr)
             (let [sym (p/sym instr :a-instr)]
               (if (Character/isDigit (first sym))
                 (format "%016d" (str-to-bin sym))
                 (format "%016d" (sym-to-bin st sym))))
             (c-to-bits instr))))
       seq))

(def st {"R0" 0, "R1" 1, "R2" 2, "R3" 3, "R4" 4, "R5" 5, "R6" 6, "R7" 7, "R8" 8, "R9" 9, "R10" 10, "R11" 11, "R12" 12, "R13" 13, "R14" 14, "R15" 15, "SP" 0, "LCL" 1, "ARG" 2, "THIS" 3, "THAT" 4, "SCREEN" 16384, "KBD" 24576})

(defn assemble [seq]
  (let [st (populate-symbol-table seq st)]
    (let [res (-> seq
                  rm-non-code
                  rm-l-instr
                  (assemble-aux st))]
      (println st)
      res)))

; (assemble '("" "// swer" "@456" "@123" "(echo)" "D=0" "0;JMP"))
; (assemble '("@456" "@123" "" "// wer" "(echo)" "D=0" "0;JMP"))
; (assemble '("(ONE)" "@ONE" "@123" "" "// wer" "(echo)" "D=0" "0;JMP"))
; (run! println (assemble '("(ONE)" "@ONE" "@123" "" "// wer" "(echo)" "D=0" "0;JMP")))

(let [fin (with-open [rdr (clojure.java.io/reader (first *command-line-args*))]
            (into [] (line-seq rdr)))]
  (spit "test.hack" (str/join "\n" (assemble fin))))
    ; (run! (.write w) (map str (assemble fin)))))
