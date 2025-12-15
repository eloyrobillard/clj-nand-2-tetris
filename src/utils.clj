(ns utils)

(defn print-seq [seq]
  (when (some? (first seq))
    (println (first seq))
    (recur (rest seq))))
