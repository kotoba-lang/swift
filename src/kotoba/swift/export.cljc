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

(def ^:private json-hex-digits "0123456789abcdef")

(defn- json-hex4
  "4-digit hex for a JSON `\\uXXXX` escape (portable: bit ops + a lookup
  table, no Long/Integer interop that would only work on :clj)."
  [n]
  (apply str (for [shift [12 8 4 0]] (nth json-hex-digits (bit-and (bit-shift-right n shift) 0xf)))))

(def ^:private json-string-escapes
  "RFC 8259 §7: EVERY control character U+0000-U+001F must be escaped in
  a JSON string, not just \\ \" and \\n -- an operator-supplied field
  containing a raw \\t, \\r, or other control byte would otherwise be
  copied through raw, producing invalid JSON (verified against Python's
  strict json module)."
  (into {\" "\\\"" \\ "\\\\"}
        (for [i (range 0x20)]
          [(char i) (case i
                      8 "\\b" 9 "\\t" 10 "\\n" 12 "\\f" 13 "\\r"
                      (str "\\u" (json-hex4 i)))])))

(defn- json-str [v]
  (str/escape (str (if (nil? v) "" v)) json-string-escapes))

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
