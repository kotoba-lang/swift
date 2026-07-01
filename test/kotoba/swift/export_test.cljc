(ns kotoba.swift.export-test
  (:require [clojure.test :refer [deftest is testing]]
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
