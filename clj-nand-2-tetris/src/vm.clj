(ns vm
  (:require [clojure.java.io])
  (:require [code-writer])
  (:require [clojure.string :as str])
  (:require [parser]))

(defn run [filename lines line-num]
  (if (nil? (first lines))
    nil
    (do
      (println (str/join " " ["//" (first lines)]))
      (loop [asms (code-writer/write filename (parser/parse (first lines) line-num))]
        (when (some? (first asms))
          (println (first asms))
          (recur (rest asms))))
      (run filename (rest lines) (+ line-num 1)))))

; (run "Foo" '("push argument 1" "add" "neg"))

(defn sanitize-lines [lines]
  (remove
   #(or
     (str/blank? %)
     (str/starts-with? (str/triml %) "//"))
   lines))

(let [filename (first *command-line-args*)]
  (with-open [r (clojure.java.io/reader filename)]
    (let [lines (sanitize-lines (into [] (line-seq r)))]
      (run filename
           lines 0))))
