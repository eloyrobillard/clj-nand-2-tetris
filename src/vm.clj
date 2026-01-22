(ns vm
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [utils])
  (:require [vm.code-writer :as cw])
  (:require [vm.parser :as p])
  (:gen-class))

(defn run [filename funcname call-num line-num lines result]
  (if (nil? (first lines))
    result
    (let [res (cw/write filename funcname call-num (p/parse (first lines) line-num))
          fname (:fname res)
          call-num (:call-num res)
          asm (:asm res)]
      (run filename fname call-num (+ line-num 1) (rest lines) (concat result [(str/join " " ["//" (first lines)])] asm)))))

(defn sanitize-lines [lines]
  (map str/triml (remove
                  #(or
                    (str/blank? %)
                    (str/starts-with? (str/triml %) "//"))
                  lines)))

(def sp-setup ["// set SP to 256" "@256" "D=A" "@SP" "M=D" "// call Sys.init if it exists" "@Sys.init" "D=A" "@16" "D=D-A" "@SkipSysInit" "D;JEQ" "// push retAddr" "@Foo$ret.1" "D=A" "// push D" "@SP" "A=M" "M=D" "@SP" "M=M+1" "// push LCL" "@LCL" "D=M" "// push D" "@SP" "A=M" "M=D" "@SP" "M=M+1" "// push ARG" "@ARG" "D=M" "// push D" "@SP" "A=M" "M=D" "@SP" "M=M+1" "// push THIS" "@THIS" "D=M" "// push D" "@SP" "A=M" "M=D" "@SP" "M=M+1" "// push THAT" "@THAT" "D=M" "// push D" "@SP" "A=M" "M=D" "@SP" "M=M+1" "// ARG = SP-5-nArgs" "// （実質 ARG = SP; ARG -= 5 + nArgs）" "@SP" "D=M" "@ARG" "M=D" "@5" "D=A" "@ARG" "M=M-D" "// LCL = SP" "@SP" "D=M" "@LCL" "M=D" "// goto Sys.init" "@Sys.init" "0;JMP" "(Foo$ret.1)" "(SkipSysInit)"])

(defn sanitize-filename [filename]
  (-> filename
      (str/replace #"\..*" "")
      (str/replace #".+\/" "")))

(defn vm-to-asm [filename lines init-count]
  (run filename "" 0 init-count lines []))

(defn vm-file-to-asm [filename init-count]
  {:pre [(string? filename)]}
  (with-open [r (io/reader filename)]
    (let [lines (sanitize-lines (into [] (line-seq r)))]
      (vm-to-asm (sanitize-filename filename) lines init-count))))

(defn vm-folder-to-asm [filenames init-count result]
  (if-not (empty? filenames)
    (let [output (vm-file-to-asm (first filenames) init-count)
          res-count (count output)]
      (vm-folder-to-asm (rest filenames) (+ init-count res-count) (flatten [result output])))
    result))

(defn to-output-file [input-filename is-dir?]
  (if-not is-dir?
    (str/replace input-filename ".vm" ".asm")
    (let [filename (last (str/split input-filename #"/"))
          ends-with-slash (= (last input-filename) "/")]
      (if ends-with-slash
        (str input-filename filename ".asm")
        (str input-filename "/" filename ".asm")))))

(defn -main [filename]
  (let [file-or-dir (io/file filename)
        init-count (count sp-setup)
        is-dir? (.isDirectory file-or-dir)
        output-filepath (to-output-file filename is-dir?)
        result (if-not (.isDirectory file-or-dir)
                 (flatten [sp-setup (vm-file-to-asm filename init-count)])
                 (vm-folder-to-asm (filter #(str/includes? %1 ".vm") (map #(.getPath %1) (.listFiles file-or-dir))) init-count sp-setup))]
    (spit output-filepath (str/join "\n" result))))
