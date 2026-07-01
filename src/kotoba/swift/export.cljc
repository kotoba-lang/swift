(ns kotoba.swift.export
  "Operator-facing export for an interbank-messaging actor.

  Renders BIC validation results, MT messages and ISO 20022 envelopes to CSV
  and JSON for compliance/audit export. Pure data → text: no network."
  (:require [clojure.string :as str]
            [kotoba.swift :as swift]))

(defn- csv-cell [v]
  (let [s (str (if (nil? v) "" v))]
    (if (re-find #"[\",\n]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn- csv-row [vals] (str/join "," (map csv-cell vals)))

(defn- json-str [v]
  (-> (str (if (nil? v) "" v))
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn bics->csv [bics]
  (str/join "\n"
    (cons (csv-row ["bic" "valid" "country" "bank" "branch"])
          (for [b bics]
            (let [r (swift/validate-bic b)
                  p (:swift/parsed r)]
              (csv-row [b
                        (if (:swift/valid? r) "yes" "no")
                        (or (:swift/country p) "")
                        (or (:swift/bank p) "")
                        (or (:swift/branch p) "")]))))))

(defn messages->csv [messages]
  (str/join "\n"
    (cons (csv-row ["mt" "valid" "category" "sender"])
          (for [m messages]
            (let [r (swift/validate-mt-message m)]
              (csv-row [(or (:swift/mt m) "")
                        (if (:swift/valid? r) "yes" "no")
                        (or (:swift/category m) "")
                        (or (get-in m [:swift/sender :swift/primary]) "")]))))))

(defn bics->json [bics]
  (str "["
       (str/join ","
                 (for [b bics]
                   (let [r (swift/validate-bic b) p (:swift/parsed r)]
                     (str "{\"bic\":\"" (json-str b) "\","
                          "\"valid\":" (if (:swift/valid? r) "true" "false") ","
                          "\"country\":\"" (json-str (:swift/country p)) "\","
                          "\"bank\":\"" (json-str (:swift/bank p)) "\"}"))))
       "]"))

(defn messages->json [messages]
  (str "["
       (str/join ","
                 (for [m messages]
                   (let [r (swift/validate-mt-message m)]
                     (str "{\"mt\":\"" (json-str (:swift/mt m)) "\","
                          "\"valid\":" (if (:swift/valid? r) "true" "false") ","
                          "\"category\":\"" (json-str (:swift/category m)) "\","
                          "\"sender\":\"" (json-str (get-in m [:swift/sender :swift/primary])) "\"}"))))
       "]"))
