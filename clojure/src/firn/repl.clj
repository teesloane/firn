(ns firn.repl
  (:require [sci.core :as sci]
            [clojure.pprint :as pprint]
            [clojure.string :as s]
            [firn.build :as build]))

;; -- REPL Functions (The Repl "API") --

(defn reload!
  "Takes the config atom, and requests it to build all files."
  [config!]
  (let [new-config (-> config! deref (build/all-files))]
    (reset! config! new-config)
    "Reloaded files."))

(defn help
  []
  (println
   (->> [""
         "The repl can help you learn about the data moving around a Firn site when it runs."
         "The following functions are available to you:"
         ""
         "# -- Bound Clojure functions:"
         "(pprint m)       ; pretty print a clojure value"

         ""
         "# -- Available Data:"
         "@config          ; de-reference and view the config atom (it's large!)"
         ""
         "# -- Custom Functions:"
         "(help)           ; print this message"
         "(reload! config) ; reload the config and rebuild your site."
         ""]
        (s/join \newline)
        )))


;; -- REPL setup.

(defn prompt [ctx]
  ;; fetch the current namespace name for printig a prompt
  (let [ns-name (sci/eval-string* ctx "(ns-name *ns*)")]
    (print (str ns-name "=> "))
    (flush)))

(defn handle-error [_ctx last-error e]
  (binding [*out* *err*] (println (ex-message e)))
  (sci/set! last-error e))

(defn sci-bindings
  [config]
  {'config  config
   'help    help
   'reload! reload!
   'pprint  pprint/pprint})

(defn init [config]
  (let [;; we are going to read Clojure expressions from stdin
        reader     (sci/reader *in*)
        last-error (sci/new-dynamic-var '*e nil {:ns (sci/create-ns 'clojure.core)})
        ctx        (sci/init {:namespaces {'clojure.core {'*e last-error}}
                              :bindings (sci-bindings config)})]
    ;; establish a thread-local bindings to allow set!
    (sci/with-bindings {sci/ns     @sci/ns
                        last-error @last-error}
      (loop []
        (prompt ctx)
        (let [;; read the next form from stdin
              next-form (try (sci/parse-next ctx reader)
                             (catch Throwable e
                               (handle-error ctx last-error e)
                               ::err))]
          (when-not (= ::sci/eof next-form)
            ;; eval the form if it's not an error and print the result
            (when-not (= ::err next-form)
              (let [res (try (sci/eval-form ctx next-form)
                             (catch Throwable e
                               (handle-error ctx last-error e)
                               ::err))]
                (when-not (= ::err res)
                  (prn res))))
            ;; repeat!
            (recur)))))))

