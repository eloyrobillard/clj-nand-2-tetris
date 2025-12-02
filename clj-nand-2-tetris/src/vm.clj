(ns vm
  (:require [clojure.java.io])
  (:require [code-writer])
  (:require [clojure.string :as str])
  (:require [parser]))

(defn print-sequence [seq]
  (when (some? (first seq))
    (println (first seq))
    (recur (rest seq))))

(defn run [filename funcname lines line-num]
  (if (nil? (first lines))
    nil
    (do
      (println (str/join " " ["//" (first lines)]))
      (let [res (code-writer/write filename funcname (parser/parse (first lines) line-num))
            fname (:fname res)
            asm (:asm res)]
        (print-sequence asm)
        (run filename fname (rest lines) (+ line-num 1))))))

(defn sanitize-lines [lines]
  (map str/triml (remove
                  #(or
                    (str/blank? %)
                    (str/starts-with? (str/triml %) "//"))
                  lines)))

(def regs-setup ["// set SP up" "@256" "D=A" "@SP" "M=D"])

(defn sanitize-filename [filename]
  (str/replace filename #"\..*" ""))

(let [filename (first *command-line-args*)]
  (with-open [r (clojure.java.io/reader filename)]
    (let [lines (sanitize-lines (into [] (line-seq r)))]
      (run (sanitize-filename filename)
           "Sys.init"
           lines 0))))
