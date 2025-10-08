# Clojure Hack Assembler

## Getting Started

### Using Neovim/Conjure

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
