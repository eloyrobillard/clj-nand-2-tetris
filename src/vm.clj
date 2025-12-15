(ns vm
  (:require [clojure.java.io])
  (:require [clojure.string :as str])
  (:require [utils])
  (:require [vm.code-writer :as cw])
  (:require [vm.parser :as p]))

(defn run [filename funcname call-num lines line-num]
  (if (nil? (first lines))
    nil
    (do
      (println (str/join " " ["//" (first lines)]))
      (let [res (cw/write filename funcname call-num (p/parse (first lines) line-num))
            fname (:fname res)
            call-num (:call-num res)
            asm (:asm res)]
        (utils/print-sequence asm)
        (run filename fname call-num (rest lines) (+ line-num 1))))))

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
           0
           lines 0))))
