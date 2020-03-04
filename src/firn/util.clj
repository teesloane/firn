(ns firn.util)

(defn get-files-of-type
  "Takes an io/file sequence and gets all files of a specific extension."
  [fileseq ext]
  (filter (fn [f]
            (and
             (.isFile f)
             (-> f .getName (.endsWith ext))))
          fileseq))

(defn exit-with-err
  "Exits with error.
  TODO: make this not exit the repl in dev-mode."
  [msg]
  (prn "Err: " msg)
  #_(System/exit 1))
