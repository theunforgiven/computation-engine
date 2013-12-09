(ns com.cyrusinnovation.computation.Clojail
  (:use [clojail core testers]
        [clojail.core :only [sandbox safe-read]]
        [clojail.testers :only [secure-tester-without-def]])
  (:gen-class
    :methods [#^{:static true} [safeEval [String] Object]]))

(def sb (sandbox secure-tester))

(defn -safeEval
  "Evaluate a string inside a clojail sandbox."
  [stringExpr]
  (sb (safe-read stringExpr)))