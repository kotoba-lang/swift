# kotoba-swift

[![CI](https://github.com/kotoba-lang/swift/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/swift/actions/workflows/ci.yml)

**SWIFT MT and ISO 20022 interbank messaging in pure Clojure.** A
[kotoba-lang](https://github.com/kotoba-lang) capability library that gives
the [`cloud-itonami-6419`](https://github.com/gftdcojp/cloud-itonami-6419)
community monetary-intermediation open business a structural model of the
messages it exchanges with the SWIFT network and ISO 20022 counterparties —
BIC identification, the SWIFT MT basic-header / text-block record, and the
ISO 20022 business-application envelope.

The library models **records, not wire format**. SWIFT MT on the wire uses
`{1:...}{4:...}` brace blocks and ISO 20022 is XML; here both are EDN so a
`PolicyGovernor`, audit ledger, or test harness can reason structurally
without a parser. No network, no I/O — portable `.cljc` across JVM /
ClojureScript / SCI / GraalVM.

## Contract

```clojure
(require '[kotoba.swift :as swift])

(swift/bic-valid? "DEUTDEFF500")            ; => true
(swift/parse-bic "DEUTDEFF500")             ; => {:swift/bank "DEUT" ...}
(swift/mt-message "103" "DEUTDEFF" {:order "CUST/123"})
(swift/iso-20022-envelope "pacs.008.001.12"
                          "DEUTDEFF" "CHASUS33" {:amt 100})
(swift/validate-bic "BAD")                  ; => {:swift/valid? false ...}
```

## Why

A community bank operator must prove, before a message is committed, that the
sender is identified, the MT type is in a permitted category, and the
envelope is structurally sound. `kotoba-swift` is the pure-data layer the
governor checks against; the actor (`cloud-itonami-6419`) decides
permission, the ledger records proof.

## License

Apache License 2.0.
