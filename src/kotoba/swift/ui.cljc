(ns kotoba.swift.ui
  "Operator-facing console for an interbank-messaging actor.

  Renders an HTML read-only panel of BIC validation and ISO 20022 / SWIFT MT
  messages, using kotoba-lang/html + css. Pure data → markup: no network.
  The governor gates dispatch; this view only observes."
  (:require [html.core :as html]
            [css.core :as css]
            [kotoba.swift :as swift]))

(def ^:private sheet
  {:rules
   {"body" {:font-family "system-ui,-apple-system,sans-serif" :margin 0 :color "#1a1a1a" :background "#fafafa"}
    "header.bar" {:display :flex :align-items :center :gap 12 :padding "12px 20px" :background "#fff" :border-bottom "1px solid #e5e5e5"}
    "header.bar h1" {:font-size 18 :margin 0 :font-weight 600}
    "header.bar .badge" {:margin-left :auto :font-size 12 :color "#666"}
    "main" {:max-width 980 :margin "24px auto" :padding "0 20px"}
    ".card" {:background "#fff" :border "1px solid #e5e5e5" :border-radius 8 :padding 16 :margin-bottom 16}
    "h2" {:margin-top 0 :font-size 15}
    "table" {:width "100%" :border-collapse :collapse :font-size 14}
    "th, td" {:text-align :left :padding "8px 10px" :border-bottom "1px solid #f0f0f0"}
    "th" {:font-weight 600 :color "#555" :font-size 12 :text-transform :uppercase :letter-spacing "0.04em"}
    ".ok" {:color "#137a3f"}
    ".err" {:color "#b3261e" :background "#fbe9e7" :padding "2px 6px" :border-radius 4}
    ".muted" {:color "#888"}
    "code" {:background "#f5f5f5" :padding "1px 5px" :border-radius 3 :font-family "ui-monospace,monospace" :font-size 13}}})

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
