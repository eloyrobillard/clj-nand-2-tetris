# Clojure Hack Assembler

## Getting Started

### Using Neovim/Conjure

Install `clj` and Clojure: [official instructions](https://clojure.org/guides/install_clojure).

Start a REPL:

``` bash
clj -M:repl/conjure
```

Conjure will automatically connect to the REPL when opening Neovim.

Motions for Conjure all start with the custom prefix `,c`.

Important motions include:

- `<prefix>eb`: Interpret the entire buffer (file);
- (in selection mode) `<prefix>E`: Interpret the selection
- `<prefix>lv`: Open log in new vertical window

Run a file using (`-M` stands for main):

``` bash
clj -M <file/path>
```

## Postmortem

### Clojure/Javaにおける意味不明なエラー Cryptic errors in Clojure/Java

#### `class <module>$<function>$sym__<number> cannot be cast to class java.lang.<Class>`

このエラーは下記のコードで連発していた：

``` clojure
(defn hoge [x y]
  (-> x
      #(fuga y %)
      ...))
```

`(->)`の[ページ](https://clojuredocs.org/clojure.core/-%3E)を調べると、匿名関数は括弧に入れないと例外が発生するそうだ。したがって、以下のようになる。

``` clojure
(defn hoge [x y]
  (-> x
      (#(fuga y %)) ; 括弧に入れる
      ...))
```

また、次の場合も例外が起こる：

``` clojure
(-> 10
    (fn [n] (/ n 2))) ; 例外
```

今回の例外の内容は違う：

```
Syntax error macroexpanding clojure.core/fn at (<address>).
10 - failed: vector? at: [:fn-tail :arity-1 :params] spec: :clojure.core.specs.alpha/param-list
10 - failed: (or (nil? %) (sequential? %)) at: [:fn-tail :arity-n] spec: :clojure.core.specs.alpha/params+body
```

直し方は変わらない：

``` clojure
(-> 10
    ((fn [n] (/ n 2)))) ; これも括弧に入れる
```

##### 原因？

正確な原因がわからないが、匿名関数を実行しない限り「本物」の関数（引数を引き受ける関数）に変換されないかもしれない。

たとえば、REPLに`#(/ % 2) 10`を入力しても返されるのは`10`だけであり、`(#(/ % 2) 10)`ならちゃんと`5`が返される。

同じく、(->)`に匿名関数をそのまま渡すと実行されず関数として解釈されていない可能性がある。
