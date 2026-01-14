(ns vm
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [utils])
  (:require [vm.code-writer :as cw])
  (:require [vm.parser :as p]))

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

; NOTE: Sys.init の存在を確認するときに使う @16 は
; Sys.init が存在しない場合に引き継いでしまうアドレス
(def sp-setup ["// set SP up if not already done by testing script" "@SP" "D=M" "@SkipSPInit" "D;JNE" "@256" "D=A" "@SP" "M=D" "(SkipSPInit)" "// call Sys.init if it exists" "@Sys.init" "D=A" "@16" "D=D-A" "@Sys.init" "D;JNE"])

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

(defn vm-folder-to-asm [filenames init-count]
  (when-not (empty? filenames)
    (let [res (vm-file-to-asm (first filenames) init-count)
          res-count (count res)]
      (utils/print-seq res)
      (vm-folder-to-asm (rest filenames) (+ init-count res-count)))))

(defn -main [filename]
  (utils/print-seq sp-setup)
  (let [fileOrDir (io/file filename)
        init-count (count sp-setup)]
    (if-not (.isDirectory fileOrDir)
      (utils/print-seq (vm-file-to-asm filename init-count))
      (vm-folder-to-asm (map #(.getPath %1) (.listFiles fileOrDir)) init-count))))
