(ns code-writer
  (:require [clojure.string :as str])
  (:require [clojure.core.match :refer [match]]))

(defn get-value [filename segment offset]
  {:pre [(string? filename) (string? segment) (string? offset)]
   :post [(sequential? %)]}
  (match segment
    "constant" [(str/join "" ["@" offset])]
    "static" [(str/join "." [(str/join "" ["@" filename]) (Integer/toString offset)]) "D=M"]
    "local" "@LCL"
    "argument" "@ARG"
    "this" "@THIS"
    "that" "@THAT"
    "temp" "@TEMP"
    "pointer" (if
               (= offset 0)
                "@THIS"
                "@THAT")))

(get-value "Foo" "static" 8)
(get-value "Foo" "constant" 8)
(get-value "Foo" "local" 8)

(defn write-push-pop [filename op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)]
    (let [value (get-value filename a1 a2)]
      (if (= type :c-push)
        []))))

(defn write-arithmetic [op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)]))

(defn write [filename op]
  (let [type (:type op)
        a1 (:a1 op)
        a2 (:a2 op)]
    (match type
      :c-arithm (write-arithmetic op)
      :else (write-push-pop filename op))))

