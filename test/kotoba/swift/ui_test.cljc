(ns kotoba.swift.ui-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.swift :as swift]
            [kotoba.swift.ui :as ui]))

(deftest dashboard-renders-contracts
  (testing "empty dashboard renders a page"
    (let [html (ui/dashboard {})]
      (is (re-find #"<html>" html))
      (is (re-find #"Operator Console" html))))
  (testing "populated dashboard renders records"
    (let [html (ui/dashboard {:bics ["DEUTDEFF500" "BAD"], :messages [(swift/mt-message "103" "DEUTDEFF" {})]})]
      (is (re-find #"✓" html))
      (is (re-find #"valid" html)))))

(deftest dashboard-is-read-only
  (testing "the console never renders a write surface"
    (let [html (ui/dashboard {:bics ["DEUTDEFF500" "BAD"], :messages [(swift/mt-message "103" "DEUTDEFF" {})]})]
      (is (re-find #"read-only · governor-gated" html))
      (is (not (re-find #"<form" html)))
      (is (not (re-find #"<button" html))))))
