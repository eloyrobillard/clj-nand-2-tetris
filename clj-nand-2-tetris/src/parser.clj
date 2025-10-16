(ns parser
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.string :as str]))

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
    "not" :c-arithm))

(defn arg1 [parts type]
  {:pre [(not (= type :c-return))]
   :post [(some? %)]}
  (parts 0))

(defn arg2 [parts type]
  {:pre [(some #(= type %) [:c-push, :c-pop, :c-function, :c-call])]
   :post [(some? %)]}
  (parts 1))

(defn parse [line]
  (let [parts (str/split line #" ")
        type (command-type (parts 0))]))

