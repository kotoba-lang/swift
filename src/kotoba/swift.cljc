(ns kotoba.swift
  "SWIFT MT and ISO 20022 interbank messaging — pure data contracts.

  A kotoba-lang capability library for the cloud-itonami-6419 (community
  monetary intermediation) open business. No network, no I/O. Models the
  message structures a banking operator exchanges with the SWIFT network and
  ISO 20022 counterparties: BIC (ISO 9362) identification, the SWIFT MT
  basic-header / text-block record, and the ISO 20022 business-application
  envelope.

  The library models records, not wire format. SWIFT MT on the wire uses
  {1:...}{4:...} brace blocks and ISO 20022 is XML; here both are EDN so a
  governor or test harness can reason structurally without a parser.

  Portable (.cljc) across JVM / ClojureScript / SCI / GraalVM."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; BIC — Bank Identifier Code (ISO 9362 / SWIFT-BIC)
;;   shape: BBBBCCLL[bbb]  bank(4) country(2 ISO-3166) location(2) branch?(3)
;; ---------------------------------------------------------------------------

(def ^:private bic-pattern #"[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?")

(defn bic-valid?
  "True when s is an 8- or 11-character alphanumeric string in SWIFT BIC shape
  (bank letters, country letters, location alnum, optional branch alnum)."
  [s]
  (and (string? s)
       (contains? #{8 11} (count s))
       (boolean (re-matches bic-pattern s))))

(defn parse-bic
  "Decompose a BIC into {:swift/bank :swift/country :swift/location
  :swift/branch :swift/primary}. Returns nil when malformed."
  [s]
  (when (bic-valid? s)
    {:swift/bank     (subs s 0 4)
     :swift/country  (subs s 4 6)
     :swift/location (subs s 6 8)
     :swift/branch   (when (= 11 (count s)) (subs s 8 11))
     :swift/primary  (subs s 0 8)}))

(defn bic-primary
  "Return the 8-character primary BIC (bank + country + location), dropping
  any branch code. Returns nil when malformed."
  [s]
  (when (bic-valid? s) (subs s 0 8)))

;; ---------------------------------------------------------------------------
;; SWIFT MT message — basic-header (block 1) + text block (block 4) contract
;; ---------------------------------------------------------------------------

(def mt-categories
  "SWIFT MT category groups relevant to a community bank, keyed by the first
  digit of the 3-digit MT type."
  {"1" "Customer Payments & Cheques"
   "2" "Financial Institution Transfers"
   "9" "Cash Management & Customer Status"})

(defn mt-type-valid?
  "True when mt is a 3-digit string whose first digit is a known category."
  [mt]
  (and (string? mt)
       (= 3 (count mt))
       (contains? mt-categories (subs mt 0 1))))

(defn mt-block-1
  "Basic header block 1 (sender identification). Modeled as a data record, not
  the wire format."
  [bic session-seq]
  {:swift/block          :block-1
   :swift/application-id "F"
   :swift/service-id     "01"
   :swift/sender-bic     (bic-primary bic)
   :swift/session-seq    session-seq})

(defn mt-message
  "Construct an MT message record. mt is a 3-digit type (\"103\", \"202\").
  bic is the sender's BIC. text-block is the block-4 payload as EDN. Returns
  nil when the MT type or BIC is invalid."
  [mt bic text-block]
  (when (and (mt-type-valid? mt) (bic-valid? bic))
    {:swift/mt         mt
     :swift/category   (mt-categories (subs mt 0 1))
     :swift/sender     (parse-bic bic)
     :swift/block-1    (mt-block-1 bic "000001")
     :swift/text-block text-block
     :swift/trailers   {}}))

;; ---------------------------------------------------------------------------
;; ISO 20022 business application header (BAH) + document contract
;;   Wire format is XML; the envelope is modeled as EDN.
;; ---------------------------------------------------------------------------

(defn iso-20022-envelope
  "Construct an ISO 20022 message envelope. namespace is the message
  identifier (e.g. \"pacs.008.001.12\"). from-bic / to-bic identify the
  sending and receiving institutions. business-data is the document payload
  as EDN. Returns nil when namespace is missing."
  [namespace from-bic to-bic business-data]
  (when (and (string? namespace) (seq namespace))
    {:swift/iso-20022     namespace
     :swift/from          (parse-bic from-bic)
     :swift/to            (parse-bic to-bic)
     :swift/business-data business-data}))

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(defn validate-bic
  "Return a validation result for a candidate BIC."
  [s]
  (cond
    (not (string? s))    {:swift/valid? false :swift/error :not-a-string}
    (not (bic-valid? s)) {:swift/valid? false :swift/error :malformed-bic}
    :else                {:swift/valid? true :swift/parsed (parse-bic s)}))

(defn validate-mt-message
  "Return a validation result for an MT message record (as produced by
  mt-message)."
  [m]
  (cond
    (not (map? m))                                       {:swift/valid? false :swift/error :not-a-map}
    (not (:swift/mt m))                                  {:swift/valid? false :swift/error :missing-mt-type}
    (not (mt-type-valid? (:swift/mt m)))                 {:swift/valid? false :swift/error :invalid-mt-type}
    (not (bic-valid? (get-in m [:swift/sender :swift/primary])))
    {:swift/valid? false :swift/error :invalid-sender-bic}
    :else                                                {:swift/valid? true :swift/mt (:swift/mt m)}))
