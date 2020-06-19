(ns firn.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [firn.build :as build]
            [clojure.tools.cli :refer [parse-opts]]
            [firn.server :as server]
            [clojure.string :as s]
            [firn.util :as u]
            [mount.core :as mount]))

(def FIRN-VERSION "0.0.5")

(defn init!
  "When firn is run as a native image, move the dependencies (the parser bin)
  to the home directory. Not ideal, but this is the best we can do for now!"
  []
  (when (u/native-image?)
    (let [home (System/getProperty "user.home")
          lib-dir (io/file home ".firn")]
      (.mkdirs lib-dir)
      (doseq [lib-name ["libmylib.dylib" "libmylib.so"]]
        (when-let [resource (io/resource lib-name)]
          (let [lib-file (io/file lib-dir lib-name)]
            (io/copy (io/input-stream resource) lib-file))))
      (System/setProperty "java.library.path" (.getPath lib-dir)))))

(defn usage [options-summary]
  (->> ["Firn - A static-site generator for org-mode."
        ""
        "Usage: firn [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  build    Build a static site in a directory with org files."
        "  new      Scaffold files and folders needed to start a new site."
        "  serve    Runs a development server for processed org files."]
       (s/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn cli-options
  []
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 4000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]
   ["-v" "--version"]
   ["-d" "--dir PATH" "Directory to build/serve"
    :default (u/get-cwd)]])

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args (cli-options))]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      (:version options)
      {:exit-message (str "Firn " FIRN-VERSION)}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"serve" "build" "new"} (first arguments)))
      {:action (first arguments) :options options}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (when-not u/dev?
    (System/exit status)))

(defn -main
  "Parses and validates command line options,
  loads the rust library."
  [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        (init!)
        (clojure.lang.RT/loadLibrary "mylib")
        (case action
          "serve"  (server/serve    options)
          "build"  (build/all-files options)
          "new"    (build/new-site  {}))))))


(mount/stop)
(-main "serve" "-d" "/Users/tees/Dropbox/wiki")
