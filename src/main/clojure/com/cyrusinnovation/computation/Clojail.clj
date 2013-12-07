(ns com.cyrusinnovation.computation.Clojail
  (:use [clojail core testers]
        [clojail.core :only [sandbox safe-read]]
        [clojail.testers :only [secure-tester-without-def]])
  (:gen-class
    :methods [#^{:static true} [safeEval [String] Object]]))

(def ^{:doc "A tester that attempts to be secure, but allows def, in-ns, Var, and Compiler."}
  function-definition-sandbox
  [(blacklist-objects [clojure.lang.Ref clojure.lang.Reflector
                       clojure.lang.Namespace clojure.lang.RT
                       java.io.ObjectInputStream])
   (blacklist-packages ["java.lang.reflect"
                        "java.security"
                        "java.util.concurrent"
                        "java.awt"])
   (blacklist-symbols
    '#{alter-var-root intern eval catch
       load-string load-reader addMethod ns-resolve resolve find-var
       *read-eval* ns-publics ns-unmap set! ns-map ns-interns the-ns
       push-thread-bindings pop-thread-bindings future-call agent send
       send-off pmap pcalls pvals System/out System/in System/err
       with-redefs-fn Class/forName})
   (blacklist-nses '[clojure.main])
   (blanket "clojail")])

(def sb (sandbox function-definition-sandbox))

(defn -safeEval
  "Evaluate a string inside a clojail sandbox."
  [stringExpr]
  (sb (safe-read stringExpr)))