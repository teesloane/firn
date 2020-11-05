(ns firn.repl
  (:require [sci.core :as sci]))

;; -- REPL Functions (The Repl "API") --

;; TODO: reload!

;; -- REPL setup.

(defn prompt [ctx]
  ;; fetch the current namespace name for printig a prompt
  (let [ns-name (sci/eval-string* ctx "(ns-name *ns*)")]
    (print (str ns-name "> "))
    (flush)))

(defn handle-error [_ctx last-error e]
  (binding [*out* *err*] (println (ex-message e)))
  (sci/set! last-error e))

(defn sci-bindings
  [config]
  {'config config})

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

