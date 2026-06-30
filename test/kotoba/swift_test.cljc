(ns kotoba.swift-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.swift :as swift]))

(deftest bic-valid-test
  (testing "accepts 8- and 11-character BICs"
    (is (swift/bic-valid? "DEUTDEFF"))
    (is (swift/bic-valid? "DEUTDEFF500"))
    (is (swift/bic-valid? "CHASUS33XXX")))
  (testing "rejects malformed BICs"
    (is (not (swift/bic-valid? "DEUT")))            ; too short
    (is (not (swift/bic-valid? "deutdeff")))        ; lowercase
    (is (not (swift/bic-valid? "DE1TDEFF")))        ; digit in bank field
    (is (not (swift/bic-valid? 42)))))              ; not a string

(deftest parse-bic-test
  (testing "11-char BIC decomposes with branch"
    (is (= {:swift/bank     "DEUT"
            :swift/country  "DE"
            :swift/location "FF"
            :swift/branch   "500"
            :swift/primary  "DEUTDEFF"}
           (swift/parse-bic "DEUTDEFF500"))))
  (testing "8-char BIC has nil branch"
    (is (nil? (:swift/branch (swift/parse-bic "DEUTDEFF")))))
  (testing "malformed returns nil"
    (is (nil? (swift/parse-bic "BAD")))))

(deftest mt-message-test
  (testing "MT103 construction"
    (let [m (swift/mt-message "103" "DEUTDEFF" {:order "CUST/123"})]
      (is (= "103" (:swift/mt m)))
      (is (= "Customer Payments & Cheques" (:swift/category m)))
      (is (= "DEUTDEFF" (get-in m [:swift/block-1 :swift/sender-bic])))))
  (testing "invalid MT type returns nil (category 3 not declared)"
    (is (nil? (swift/mt-message "399" "DEUTDEFF" {}))))
  (testing "invalid BIC returns nil"
    (is (nil? (swift/mt-message "103" "BAD" {})))))

(deftest iso-20022-envelope-test
  (testing "envelope carries namespace and parties"
    (let [e (swift/iso-20022-envelope "pacs.008.001.12"
                                       "DEUTDEFF" "CHASUS33" {:amt 100})]
      (is (= "pacs.008.001.12" (:swift/iso-20022 e)))
      (is (= "DEUT" (get-in e [:swift/from :swift/bank])))
      (is (= "CHAS" (get-in e [:swift/to :swift/bank])))))
  (testing "missing namespace returns nil"
    (is (nil? (swift/iso-20022-envelope "" "DEUTDEFF" "CHASUS33" {})))))

(deftest validate-bic-test
  (testing "valid BIC returns parsed record"
    (is (true? (:swift/valid? (swift/validate-bic "DEUTDEFF500")))))
  (testing "malformed BIC returns error"
    (is (= :malformed-bic (:swift/error (swift/validate-bic "BAD"))))))

(deftest validate-mt-message-test
  (testing "round-trips a constructed MT103"
    (let [m (swift/mt-message "103" "DEUTDEFF" {})]
      (is (true? (:swift/valid? (swift/validate-mt-message m))))))
  (testing "rejects a non-map"
    (is (= :not-a-map (:swift/error (swift/validate-mt-message "x"))))))
