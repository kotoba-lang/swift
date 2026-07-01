(ns kotoba.swift.ui
  "Operator-facing console for an interbank-messaging actor.

  Renders an HTML read-only panel of BIC validation and ISO 20022 / SWIFT MT
  messages, using kotoba-lang/html + css. Pure data → markup: no network.
  The governor gates dispatch; this view only observes."
  (:require [html.core :as html]
            [css.core :as css]
            [kotoba.swift :as swift]))

;; Domain-specific rules layered on top of the shared operator-theme (css.core).
(def ^:private extra-rules
  {})

(def ^:private sheet (css/merge-theme extra-rules))

(defn- stylesheet [] (html/->html (css/style-node sheet)))

(defn- bic-rows [bics]
  (for [b bics]
    (let [r (swift/validate-bic b)]
      [:tr [:td (if (:swift/valid? r) [:span.ok "✓"] [:span.err "✕"])]
           [:td (str b)]
           [:td (or (:swift/country (:swift/parsed r)) "—")]
           [:td (or (:swift/bank (:swift/parsed r)) "—")]
           [:td (or (:swift/branch (:swift/parsed r)) "—")]])))

(defn- mt-rows [messages]
  (for [m messages]
    (let [r (swift/validate-mt-message m)]
      [:tr [:td (or (:swift/mt m) "—")]
           [:td (if (:swift/valid? r) [:span.ok "valid"] [:span.err (name (:swift/error r "—"))])]
           [:td (or (:swift/category m) "—")]
           [:td (or (get-in m [:swift/sender :swift/primary]) "—")]
           [:td (or (get-in m [:swift/to :swift/primary]) "—")]])))

(defn dashboard
  "Render a full HTML console for a SWIFT / ISO 20022 operator."
  [{:keys [bics messages envelopes] :as ctx}]
  (html/->html
    [:html
     [:head [:meta {:charset "utf-8"}] [:title "cloud-itonami · swift"]
      [:hiccup/raw (stylesheet)]]
     [:body
      [:header.bar [:h1 "Interbank Messaging — Operator Console"] [:span.badge "read-only · governor-gated"]]
      [:main
       (when (seq bics)
         [:section.card [:h2 "BIC validation"]
          [:table [:thead [:tr [:th ""] [:th "BIC"] [:th "Country"] [:th "Bank"] [:th "Branch"]]]
           [:tbody (bic-rows bics)]]])
       (when (seq messages)
         [:section.card [:h2 "MT messages"]
          [:table [:thead [:tr [:th "MT"] [:th "Status"] [:th "Category"] [:th "Sender"] [:th "Recipient"]]]
           [:tbody (mt-rows messages)]]])
       (when (seq envelopes)
         [:section.card [:h2 "ISO 20022 envelopes"]
          [:table [:thead [:tr [:th "Namespace"] [:th "From"] [:th "To"]]]
           [:tbody (for [e envelopes]
                     [:tr [:td (or (:swift/iso-20022 e) "—")]
                          [:td (or (get-in e [:swift/from :swift/primary]) "—")]
                          [:td (or (get-in e [:swift/to :swift/primary]) "—")]])]]])]]]))
