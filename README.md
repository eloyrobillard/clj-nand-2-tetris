# Clojure Hack Assembler

[NAND2TETRIS](https://www.nand2tetris.org/) コースに登場する アセンブリから Hack 機械語へのアセンブラー。

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

## 備忘録

### Clojureの独特な機能 Clojure's special features

#### 契約プログラミング

Clojure で [condition map](https://clojure.org/reference/special_forms#_fn_name_param_condition_map_expr) という機能を使い、
関数の定義に引数・返り値に対する assert を組み込むができる。

``` clojure
(defn kelvin-to-celsius [K]
  {:pre [(>= K 0)]     ; 負数のケルビンはありえない
  :post [(float? %)]}  ; 返り値はかならず浮動小数点数になる
  (+ K 273.15))
```

アサーションの利点の一つは、あとから単体テストでアサーションの内容を確認する必要をなくす。
condition map により、引数・返り値の値に契約を課し、バグが起こりうる箇所を簡単に減らすことができる。

### Clojure/Javaにおける意味不明なエラー Cryptic errors in Clojure/Java

#### `No matching method write found taking 1 args for class java.io.BufferedWriter`

"w.txt" に "true" を書き込みたく、以下のコードを書いたとしよう。

``` clojure
(with-open [w (clojure.java.io/writer "w.txt")]
  (.write w true))
```

このコードを実行すると次の例外が投げられる：

```
Execution error (IllegalArgumentException) at user/eval8965 (REPL:1).
No matching method write found taking 1 args for class java.io.BufferedWriter
```

この例外によれば、`java.io.BufferedWriter`である`w`に`write`メソッドがないというような内容に聞こえる。

だが、実は大事なのは１行目の`IllegalArgumentException`であり、本当のエラーを示す。

*すなわち、`BufferedWriter`の`write`にブールを渡すのは間違いだということ。*

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
