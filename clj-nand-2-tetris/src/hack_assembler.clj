(ns hack-assembler
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [code :as c])
  (:require [parser :as p]))

(defn rm-non-code-aux [acc seq]
  (let [head (first seq)]
    (cond
      (nil? head) acc
      (or (p/is-ws? head) (p/is-cmt? head)) (recur acc (rest seq))
      :else (recur (cons head acc) (rest seq)))))

(defn rm-non-code [seq]
  (reverse (rm-non-code-aux '() seq)))

; (rm-non-code '())
; (rm-non-code '("" "// sdf" "sdf" "// ewr" ""))
; (rm-non-code '("@456" "@123" "" "// wer" "@echo" "D=0" "0;JMP"))

(defn c-to-bits [c-instr]
  (let [dest (p/dest c-instr)
        comp (p/cmp c-instr)
        jmp (p/jump c-instr)]
    (str/join "" ["111" (c/cmp comp) (c/dest dest) (c/jump jmp)])))

(defn assemble-aux [seq]
  (when (some? (first seq))
    (let [instr (first seq)
          type (p/instruction-type instr)]
      (if (or (= type :a-instr) (= type :l-instr))
        (println (p/sym type instr))
        (println (c-to-bits instr))))
    (recur (p/advance seq))))

(defn assemble [seq]
  (let [sq (rm-non-code seq)]
    (assemble-aux sq)))

; (assemble '("" "// swer" "@456" "@123" "(echo)" "D=0" "0;JMP"))
; (assemble '("@456" "@123" "" "// wer" "(echo)" "D=0" "0;JMP"))

(let [file (with-open [rdr (clojure.java.io/reader (first *command-line-args*))]
             (into [] (line-seq rdr)))] (assemble file))


