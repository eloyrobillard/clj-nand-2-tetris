(ns utils)

(defn print-sequence [seq]
  (when (some? (first seq))
    (println (first seq))
    (recur (rest seq))))
