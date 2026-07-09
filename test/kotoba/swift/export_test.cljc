(ns kotoba.swift.export-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [kotoba.swift :as swift]
            [kotoba.swift.export :as ex]))
(deftest csv-export
  (let [csv (ex/bics->csv ["DEUTDEFF500" "BAD"])]
    (is (re-find #"bic,valid,country,bank,branch" csv))
    (is (re-find #"DEUTDEFF500,yes" csv))
    (is (re-find #"BAD,no" csv))))
(deftest json-export
  (let [j (ex/bics->json ["DEUTDEFF500"])]
    (is (re-find #"\"bic\":\"DEUTDEFF500\"" j))
    (is (re-find #"\"valid\":true" j))))
(deftest json-export-escapes-every-c0-control-character
  ;; RFC 8259 requires EVERY control character U+0000-U+001F to be
  ;; escaped, not just \ " and \n -- messages->json reads its message
  ;; map's fields directly without re-validating them, so a raw tab or
  ;; other control byte in :swift/mt would otherwise be copied through
  ;; raw, producing invalid JSON (verified against Python's strict json
  ;; module).
  (let [m [{:swift/mt (str "103" (char 9) "x" (char 1) "y")
            :swift/category "Customer"
            :swift/sender {:swift/primary "SENDBIC"}}]
        j (ex/messages->json m)]
    (is (str/includes? j "\"mt\":\"103\\tx\\u0001y\""))))
