(ns parser
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as str]))

; push segment index
; pop segment index
; no args: add, sub, neg, eq, gt, lt, and, or, not

(defn command-type [op]
  (match op
    "push" :c-push
    "pop" :c-pop
    "add" :c-arithm
    "sub" :c-arithm
    "neg" :c-arithm
    "eq" :c-arithm
    "gt" :c-arithm
    "lt" :c-arithm
    "and" :c-arithm
    "or" :c-arithm
    "not" :c-arithm
    :else :c-bad-type))

(defn arg1 [parts type]
  {:pre [(not (= type :c-return))]
   :post [(some? %)]}
  (if (= type :c-arithm)
    (parts 0)
    (parts 1)))

; (arg1 ["add" "5"] :c-arithm)
; (arg1 ["push" "constant" "9"] :c-push)

(defn arg2 [parts type]
  {:pre [(some #(= type %) [:c-push, :c-pop, :c-function, :c-call])]
   :post [(some? %)]}
  (parts 2))

(defn parse [line]
  {:pre [(string? line)]}
  (let [parts (str/split line #" ")
        type (command-type (parts 0))
        a1 (arg1 parts type)]
    (if (some #(= type %) [:c-push, :c-pop, :c-function, :c-call])
      (let [a2 (arg2 parts type)]
        {:type type :a1 a1 :a2 a2})
      {:type type :a1 a1 :a2 nil})))

