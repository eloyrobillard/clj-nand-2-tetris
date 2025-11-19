(ns parser
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as str]))

; push segment index
; pop segment index
; no args: add, sub, neg, eq, gt, lt, and, or, not

(defn command-type [op]
  (match op
    "label" :c-label
    "goto" :c-goto
    "if-goto" :c-if
    "function" :c-function
    "return" :c-return
    "call" :c-call
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

(defn arg2 [parts type]
  {:pre [(some #(= type %) [:c-push, :c-pop, :c-function, :c-call])]
   :post [(some? %)]}
  (parts 2))

(defn parse [line line-num]
  {:pre [(string? line)]}
  (let [parts (str/split line #" ")
        type (command-type (parts 0))
        a1 (arg1 parts type)]
    (if (some #(= type %) [:c-push, :c-pop, :c-function, :c-call])
      (let [a2 (arg2 parts type)]
        {:type type :a1 a1 :a2 a2 :ln line-num})
      {:type type :a1 a1 :a2 nil :ln line-num})))

