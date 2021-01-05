(ns firn.util
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [sci.core :as sci]
            [clojure.string :as str])
  (:import (java.lang Integer)
           (java.time LocalDate)))

(set! *warn-on-reflection* true)

(def sci-bindings {:bindings {'println println
                              'prn prn
                              }})

;; Some of these are borrowed from me.raynes.fs because I need to add ;; type hints for GraalVM

(def dev? (if (= (System/getenv "DEV") "TRUE") true false))

(defn print-err!
  "A custom error function.
  Prints errors, expecting a type to specified (:warning, :error etc.)
  Currently, also returns false after printing error message, so we can
  use that for control flow or for tests."
  [typ & args]
  (let [err-types   {:warning       "🚧 Warning:"
                     :error         "❗ Error:"
                     :uncategorized "🗒 Uncategorized Error:"}
        sel-log-typ (get err-types typ (get err-types :uncategorized))]
    (apply println sel-log-typ args)
    (when (and (not dev?) (= typ :error))
      (System/exit 1))))

(def ^{:doc "Current working directory. This cannot be changed in the JVM.
             Changing this will only change the working directory for functions
             in this library."
       :dynamic true}
  *cwd* (.getCanonicalFile (io/file ".")))

(defn ^java.io.File file
  "If `path` is a period, replaces it with cwd and creates a new File object
   out of it and `paths`. Or, if the resulting File object does not constitute
   an absolute path, makes it absolutely by creating a new File object out of
   the `paths` and cwd."
  [path & paths]
  (when-let [path (apply io/file (if (= path ".") *cwd* path) paths)]
    (if (.isAbsolute ^java.io.File path)
      path
      (io/file *cwd* path))))

(defn find-files*
  "Find files in `path` by `pred`."
  [path pred]
  (filter pred (-> path file file-seq)))

(defn find-files
  "Find files matching given `pattern`."
  [path pattern]
  (find-files* path #(re-matches pattern (.getName ^java.io.File %))))

(defn get-file-io-name
  "Returns the name of a file from the Java ioFile object w/o an extension."
  [f]
  (let [f-name (.getName ^java.io.File f)]
    (-> f-name (s/split #"\.") (first))))

(defn find-files-by-ext
  "Traverses a directory for all files of a specific extension."
  [dir ext]
  (let [ext-regex (re-pattern (str "^.*\\.(" ext ")$"))
        files     (find-files dir ext-regex)]
    (if (= 0 (count files))
      ;; NOTE: taking out this notifications - not sure it's necessary.
      (do #_(print-err! :warning "No" ext "files found at " dir) files)
      files)))

(defn file-name-no-ext
  "Removes an extension from a filename"
  [io-file]
  (let [f (.getName ^java.io.File io-file)]
    (-> f (s/split #"\.") (first))))

(defn get-cwd
  "Because *fs/cwd* gives out the at-the-time jvm path. this works with graal."
  []
  (System/getProperty "user.dir"))

(defn get-os
  "Return the name of the host operating system."
  []
  (str/replace (str/lower-case (System/getProperty "os.name"))
               #" " ""))

(defn snake->kebab
  "Convert strings with underscores to hyphens."
  ([s]
   (s/replace s #"_" "-"))
  ([s key-it?]
   (-> s
       (s/replace #"_" "-")
       (s/replace #" " "-")
       (keyword))))

(defn prepend-vec
  [item vector]
  (vec (cons item vector)))

(defn io-file->keyword
  "Turn a filename into a keyword."
  [io-file]
  (-> io-file file-name-no-ext (snake->kebab :key-it)))

(defn keyword->web-path
  [kw]
  (str "/" (name kw)))

(defn keyword->normal-text
  [kw]
  (-> kw name
      (s/replace #"-" " ")
      (s/replace #"_" " ")
      (s/capitalize)))

;; File Path fns ----
;; Mostly for operating on paths: `file/paths/woo/hoo.org`

(defn remove-ext
  "removes an extension from a string.
  Optionally, you can specify to only do so for specified extensions."
  ([s]
   (-> s (s/split #"\.") first))
  ([s ext]
   (let [split (s/split s #"\.")
         filename (first split)
         -ext (last split)]
     (if (= ext -ext) filename s))))

(defn read-and-eval-clj
  [io-file]
  (let [file-path (.getPath ^java.io.File io-file)
        eval-file (-> file-path slurp (sci/eval-string sci-bindings))]
    eval-file))

(defn load-fns-into-map
  "Takes a list of files and returns a map of filenames as :keywords -> file
  NOTE: It also EVALS (using sci) the files so they are in memory functions!

  so:                  `[my-file.clj my-layout.clj]`
  ------------------------------- ▼ ▼ ▼ ----------------------------------------
  becomes:    {:my-file fn-evald-1, :my-layout fn-evald-2}"

  [file-list]
  (let [file-path #(.getPath ^java.io.File %)
        eval-file #(-> % file-path slurp (sci/eval-string sci-bindings))]
    (into {} (map #(hash-map (io-file->keyword %) (eval-file %)) file-list))))

(defn read-clj
  "Reads a folder full of clj files, such as partials or layouts.
  pass a symbol for dir to request a specific folder."
  [dir {:keys [dir-partials dir-layouts dir-pages]}]
  (case dir
    :layouts
    (-> dir-layouts (find-files-by-ext "clj") (load-fns-into-map))

    :partials
    (-> dir-partials (find-files-by-ext "clj") (load-fns-into-map))

    :pages
    (-> dir-pages (find-files-by-ext "clj") (load-fns-into-map))

    (throw (Exception. "Ensure you are passing the right possible keywords to read-clj."))))

(defn dupe-name-in-dir-path?
  "Takes a str path of a directory and checks if a folder name appears more than
  once in the path"
  [dir-path dir-name]
  (> (get (frequencies (s/split dir-path #"/")) dir-name) 1))

(defn get-differing-path
  "compares two paths; returns `path-b` when it diverges from matching path-a
  ex: (get-differing-path `x/y/bar/jo` `x/y/bar/jo/bru/brunt`) => /bru/brunt"
  [path-a path-b]
  (let [split-a (s/split path-a #"/")
        split-b (s/split path-b #"/")]
    (loop [list-a split-a
           list-b split-b]
      (if-not (= (first list-a) (first list-b))
        (s/join "/" list-b)
        (recur (rest list-a) (rest list-b))))))

(defn drop-path-until
  [path until]
  (let [split-path (s/split path #"/")
        res        (drop-while #(not= % until) split-path)]
    (s/join "/" res)))

(defn build-web-path
  "A highly specific link builder (ಥ﹏ಥ)
  Builds links between files, has to handle links between parent (ie, `../../`)
  directories, as well as flat level links.

  the `file-path` is a string where the last item following the trailing slash
  is the current path that is being visited (and is thus truncated.)

  `file-path` -> bar/foo/test
  `org-link`  -> test
  `(build-web-path 'foo/boo/bar' '../test')`

  or

  `file-path` -> bar/foo/test ;
  `org-link`  -> my-file ==> bar/foo/my-file
  (build-web-path 'foo/boo/bar' 'test') =>  foo/boo/test
  "
  [file-path org-link]
  (when (and file-path org-link)
    (let [split-file     (drop-last (s/split file-path #"/"))
          split-org-link (s/split org-link #"/")]
      (loop [a split-file
             b split-org-link]
        (let [y  (first b)
              ys (rest b)]
          (if (= y "..")
            (recur (drop-last a) ys)
            (if (seq a)
              (str (s/join "/" a) "/" (s/join "/" b))
              (s/join "/" b))))))))

(defn is-attachment?
  "Checks is a path is an attachment; a local file that is not an org file."
  [path]
  (let [local-file-rgx #"(file:)(.*)\.(jpg|JPG|gif|GIF|png)"]
    (re-matches local-file-rgx path)))

;; General fns ----

(defn prompt?
  [p]
  (print p)
  (print " [Y/n]: ")
  (flush)
  (let [x (read-line)]
    (if (= x "Y") true false)))

(defn find-index-of
  "Finds the index of an item that matches a predicate."
  [pred sequence]
  (first (keep-indexed (fn [i x] (when (pred x) i))
                       sequence)))

(defn find-first
  "Find the first item in a collection."
  [f coll]
  (first (filter f coll)))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn distinct-by
  [coll key]
  (map first (vals (group-by key coll))))

(defn interpose+tail
  "Interposes a keywords and ensures the key is at the end of the list"
  [lst k]
  (vec (concat (interpose k lst) [k])))

(defn org-keyword->vector
  "Reads org frontmatter and converts strings into a vector"
  [s]
  ;; see: https://stackoverflow.com/a/40120309
  (let [the-beast #"\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?"
        as-vec    (-> s str/trim (str/split the-beast))
        cleaned   (map #(str/replace % #"\"" "") as-vec)]
    (vec cleaned)))

(defn take-while-after-first
  [pred lst]
  (let [head (first lst)
        tail (take-while pred (rest lst))]
    (concat [head] tail)))

(defn sort-map-of-lists-of-maps
  [{:keys [sort-key coll] :as opts}]
  (let [sort-method (opts :sort-by)]
    (->> coll
         (map (fn [[k v]]
                (let [sorted (sort-by (juxt nil? sort-key) v)]
                  (hash-map k
                            (if (= sort-method :newest)
                              (reverse sorted)
                              sorted)))))
         (into {}))))

(defn mapply
  "Convert a map into a list of kwaargs: https://stackoverflow.com/a/19430023"
  [f & args]
  (apply f (apply concat (butlast args) (last args))))

(defn map->args
  "Convert a map into a list of kwaargs: https://stackoverflow.com/a/19430023"
  [m]
  (apply concat m))



;; For interception thread macros and enabling printing the passed in value.
(def spy #(do (println "DEBUG:" %) %))

(defn java-date->unix-ts
  [date]
  (int (/ (.getTime ^java.util.Date date) 1000)))

(defn strip-org-date
  "<2020-05-14 19:11> -> 2020-05-14 19:11"
  [org-date]
  (-> org-date
      (s/replace #"\]" "")
      (s/replace #"\[" "")
      (s/replace #"<" "")
      (s/replace #">" "")))

(defn org-date->java-date
  "Converts <2020-02-25 05:51> -> java..."
  [org-date]
  (let [parse-fmt (java.text.SimpleDateFormat. "yyyy-MM-dd")
        parse-fn  (fn [s] (.parse ^java.text.SimpleDateFormat parse-fmt s))]
    (-> org-date
        strip-org-date
        parse-fn)))

(defn org-date->ts
  [org-date]
  (-> org-date
      strip-org-date
      (org-date->java-date)
      java-date->unix-ts))

(defn native-image?
  "Check if we are in the native-image or REPL."
  []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn str->keywrd
  "Converts a string to a keyword"
  [& args]
  (keyword (apply str args)))

(defn clean-anchor
  "converts `::*My Heading` => #my-heading
  NOTE: This could be a future problem area; ex: forwards slashes have to be
  replaced, otherwise they break the html rendering, thus
  'my heading / example -> my-heading--example
  Future chars to watch out for: `>` `<` `&` `!`"
  [anchor]
  (str "#" (-> anchor
               (s/replace #"::\*" "")
               (s/replace #"\/" "")
               (s/replace #"\." "")
               (s/replace #" " "-")
               (s/lower-case))))

;; -- String Functions ----
(defn get-web-path
  "Determines the web path of the file from the cwd.
  `dirname-files`: demo_org
  `file-path-abs`: /Users/tees/Projects/firn/firn/test/firn/demo_org/jam/jo/foo/file2.org
  `returns`      : jam/jo/foo/file2

  NOTE: currently, you cannot have the `name` of your folder of org
  files appear earlier in the path to those files.
  invalid example: `/users/foo/my-wiki/another-dir/my-wiki/file1.org`"

  [dirname-files file-path-abs]
  (if (dupe-name-in-dir-path? file-path-abs dirname-files)
    (print-err! :error "\nWell, well, well. You've stumbled into one of weird edge cases of using Firn. \nCongrats on getting here! Let's look at what's happening. \n\nThe directory of your org files appears twice in it's path:\n\n<<" file-path-abs ">>\n\nIn order to properly build web-paths for your files, Firn needs to know where your 'web-root' is. \nWe cannot currently detect which folder is your file root. \nTo solve this, either rename your directory of org files: \n\n<<" dirname-files ">>\n\nor rename the earlier instance in the path of the same name.")
    (->> (s/split file-path-abs #"/")
       (drop-while #(not (= % dirname-files)))
       rest
       (s/join "/")
       (remove-ext))))

;; Time ------------------------------------------------------------------------
;; NOTE: timestr->hours-min + timevec->time-str could use better input testing?
;; At the very least, `Integer.` is an opportunity for errors when parsing.


(defn parse-int [number-string]
  (try (Integer/parseInt number-string)
       (catch Exception e nil)))

(defn timestr->hours-min
  "Splits `1:36` -> [1 36]"
  [tstr]
  (let [split   (s/split tstr #":")
        hours   (parse-int (first split))
        minutes (parse-int (second split))]
    [hours minutes]))

(defn timestr->minutes
  "convert `03:25` into minutes 205"
  [tstr]
  (let [[h m] (timestr->hours-min tstr)]
    (+ (* h 60) m)))

(defn timestr->hour-float
  "Converts `03:25` -> `3.41` "
  [tstr]
  (double
   (/ (int (* 100 (/ (timestr->minutes tstr) 60))) 100)))

(defn timevec->time-str
  "Converts a vector of hours and minutes into readable time string.
  `[3 94]` > `4:34`"
  [[hours min]]
  (let [min->hrs       (int (Math/floor (/ min 60)))
        total-hours    (+ hours min->hrs)
        left-over-mins (mod  min 60)]
    (format "%d:%02d" total-hours left-over-mins)))

(defn timestr->add-time
  "(timestr->add-time `10:02` `00:02`) =>  10:04"
  [existing-ts to-add]
  (let [[eh em]   (timestr->hours-min existing-ts)
        [tah tam] (timestr->hours-min to-add)]
    (timevec->time-str [(+ eh tah) (+ em tam)])))

(defn date-make
  [^Integer y ^Integer m ^Integer d]
  (LocalDate/of y m d))

(defn date-str
  [date]
  (.toString ^java.time.LocalDate date))

(defn date-range
  "Creates a range of dates between date A and date B.
  (date-range [])"
  [[sy sm sd] [ey em ed]]
  (let [s-date (date-make  sy sm sd)
        e-date (date-make ey em ed)]
    (loop [curr-day s-date
           range    [s-date]]
      (if (= curr-day e-date)
        (drop-last range)
        (let [new-d (.plusDays ^java.time.LocalDate curr-day 1)]
          (recur new-d (conj range new-d)))))))

(defn build-year
  "constructs a vector maps, representing 365 days;
  Each map is (in a later function) updated with whatever logbook entries
  match on the same day"
  [year]
  (let [dates-of-year (date-range [year 1 1] [(inc year) 1 1])
        build-days    #(hash-map :date %
                                 :date-str (date-str %)
                                 :log-count 0
                                 :logs-raw  []
                                 :log-sum   "00:00"
                                 :hour-sum   0)]

    (->> dates-of-year (map build-days) vec)))

