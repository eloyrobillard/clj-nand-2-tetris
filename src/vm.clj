(ns vm
  (:require [clojure.java.io])
  (:require [clojure.string :as str])
  (:require [utils])
  (:require [vm.code-writer :as cw])
  (:require [vm.parser :as p]))

(defn run [filename funcname call-num line-num lines result]
  (if (nil? (first lines))
    result
    (let [res (cw/write filename funcname call-num (p/parse (first lines) line-num))
          fname (:fname res)
          call-num (:call-num res)
          asm (:asm res)]
      (run filename fname call-num (+ line-num 1) (rest lines) (concat result [(str/join " " ["//" (first lines)])] asm)))))

(defn sanitize-lines [lines]
  (map str/triml (remove
                  #(or
                    (str/blank? %)
                    (str/starts-with? (str/triml %) "//"))
                  lines)))

(def sp-setup ["// set SP up" "@256" "D=A" "@SP" "M=D"])

(defn sanitize-filename [filename]
  (-> filename
      (str/replace #"\..*" "")
      (str/replace #".+\/" "")))

(defn vm-to-asm [filename]
  (with-open [r (clojure.java.io/reader filename)]
    (let [lines (sanitize-lines (into [] (line-seq r)))]
      (run (sanitize-filename filename) "" 0 0 lines sp-setup))))

(let [filename (first *command-line-args*)]
  (utils/print-seq (vm-to-asm filename)))
