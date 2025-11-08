(ns vm
  (:require [clojure.java.io])
  (:require [code-writer])
  (:require [clojure.string :as str])
  (:require [parser]))

(defn run [filename lines]
  (if (nil? (first lines))
    nil
    (do
      (println (str/join " " ["//" (first lines)]))
      (loop [asms (code-writer/write filename (parser/parse (first lines)))]
        (when (some? (first asms))
          (println (first asms))
          (recur (rest asms))))
      (run filename (rest lines)))))

; (run "Foo" '("push argument 1" "add" "neg"))

(let [filename (first *command-line-args*)]
  (with-open [r (clojure.java.io/reader filename)]
    (run filename (into [] (line-seq r)))))
