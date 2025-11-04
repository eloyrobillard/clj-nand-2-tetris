(ns vm
  (:require [clojure.java.io])
  (:require [parser]))

(defn run [filename lines]
  (if (nil? (first lines)) nil
      (do
        (println (parser/parse (first lines)))
        (run filename (rest lines)))))

(run "Foo" '("push argument 1" "add" "neg"))

(with-open [filename (first *command-line-args*)
            r (clojure.java.io/reader filename)]
  (run filename (into [] (line-seq r))))
