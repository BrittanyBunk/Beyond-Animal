"
stockscraper is used to identify vegan stocks on various exchanges.
finance.yahoo profile descriptions are searched for keywords contained in yays and nays csv files.
the frequency of each word in each list are totalled, giving yay and nay column counts.
the ds is sorted by the yay column, so as to give priority to yay items regardless of nay counts.

core setup the initial version with columns Symbol Company yay nay Desc
core2 will not only count the yay nay, but also highlight the actual words in the Desc
plan for doing this:
1. gather each keyword found into vector
2. count the number in vector for total
3. change the vector into set for uniqueness
"

(ns stockscraper.core
  "namespace for stockscraper with required libraries"
  (:require
   [clojure.string :as s]
   [hiccup.core :as hc]
   [net.cgrand.enlive-html :as html]
   [tech.ml.dataset :as ds]
   [tech.v2.datatype :as dtype]
   [tech.v2.datatype.functional :as dfn]))


;; useful functions

(defn csv-read
  "reads a csv file given filename using techascent"
  [filepath]
  (first (ds/value-reader
          (ds/->dataset filepath {:header-row? false}))))

(defn join-regex
  "joins regex strings: patterns -> str -> re-pattern"
  [& patterns]
  (re-pattern (apply str
                     (map #(str %) patterns))))


;; definitions of keywords and exchanges

(def mays
  (csv-read "resources/may.txt"))
(def nays
  (csv-read "resources/nay.txt"))
(def yays
  (csv-read "resources/yay.txt"))
(def exchs
  ["AMEX" "ASX" "NASDAQ" "NYSE" "SGX" "TSX" "TSXV"])


;; get the descriptions from the yahoo finance profiles of a stock

(defn mkurl
  "creates a url on finance.yahoo using stock symbol"
  [sym]
  (str "https://finance.yahoo.com/quote/" sym "/profile"))

;; this works but leaves 503 stocks unprocessed
(defn fetch-url
  "parses the html from the url provided"
  [url]
  (html/html-resource
   (try
     (java.net.URL. url)
     (catch Exception e (prn (str "ERROR: url " (.toString e)))))))

#_(defn retry-fetch
  "keeps retrying to fetch page contents to bypass 503 error"
  [fn url]
  (let [res (try
              {:value (fn url)}
              (catch Exception e
                {:exception e}))]
    (if (:exception res)
      (recur fn url)
      (:value res))))

#_(defn fetch-url
  "parses the html from the url provided"
  [url]
  (html/html-resource
   (retry-fetch java.net.URL. url)))

#_(defn fetch-url
  "parses the html from the url provided"
  [url]
  (loop [tries 6]
     (when (try
             (html/html-resource (java.net.URL. url))
             false
             (catch Exception e
               ;;(prn (.getMessage e))
               (pos? tries))
             )
       (recur (dec tries))))
)

(defn pull-desc
  "pulls the description of the stock from the parsed text"
  [parsedtxt]
  (html/text
   (first (html/select parsedtxt
                       [(html/attr-contains :class "Mt(15px) Lh(1.6)")]))))

(defn stock-desc
  "returns stock description when possible or blank string"
  [symb comp]
  (let [txt (fetch-url (mkurl symb))
        title (first (map html/text (html/select txt [:title])))
        lookup? (re-find #"^Symbol Lookup" title)]
    #_(Thread/sleep 6000)
    (pr symb)
    (if-not lookup?
      (pull-desc txt)
      "")))


;; dataset processing

(defn countall
  "given (empty) desc and keywords finds count of all word occurences of latter"
  [desc ays]
  (loop [tot 0
         ays ays]
    (if (empty? ays)
      tot
      (recur
       (+ tot (count
               (re-seq (re-pattern
                        (join-regex "\\s" (first ays) "[\\s,.?!]")) desc))) ;confirm it is a word
       (rest ays)))))

(defn mk-kwd-v
  "given desc and kwds produces kwd vector of all matches"
  [desc ays]
  (loop [kv []
         ays ays]
    (if (empty? ays)
      (flatten (remove nil? kv))
      (recur
       (conj kv
             (re-seq (re-pattern
                      (join-regex "\\b" "(?i)" (first ays) "\\b"))
                     desc))
       (rest ays)))))

(defn ds-filtered
  "create the ds filtering out blank desc"
  [ds]
  (let [ds0 (assoc ds :desc
                   (map stock-desc (ds :symbol) (ds :company)))
        ds1 (ds/filter-column #(not= "" %) :desc ds0)
        ds2 (assoc ds1 :yay
                   (map #(mk-kwd-v % yays) (ds1 :desc)))
        ds3 (assoc ds2 :nay
                   (map #(mk-kwd-v % nays) (ds2 :desc)))
        ds4 (assoc ds3 :yayk
                   (map count (ds3 :yay)))
        ds5 (assoc ds4 :nayk
                   (map count (ds4 :nay)))
        ds6 (assoc ds5 :may
                   (map #(mk-kwd-v % mays) (ds5 :desc)))
        ds7 (assoc ds6 :mayk
                   (map count (ds6 :may)))]
    (ds/sort-by-column :yayk > ds7)))

(defn is-a-stock
  "identifies if a stock from company description"
  [comp]
  (when-not (re-find #"ETF|ETN|Ind|Bond|Proshares" comp)
    true))

(defn ds-final
  "creates dataset for an exchange"
  [exch]
  (let [exch-fil (str exch ".txt")
        exch-tsv (str exch ".tsv")
        full-list (-> (ds/->dataset (str "resources/" exch-fil))
                      (ds/rename-columns {"Symbol" :symbol
                                          "Description" :company}))
        stocks-list (ds/filter-column is-a-stock :company full-list)]
    (prn (str "working " exch))
    ;;(ds-tsv (ds-filtered stocks-list) exch-tsv)
    (ds-filtered stocks-list)))


;; html page generation

(defn color-me
  "colors the ays in the description"
  [txt ays color]
  (loop [txt txt
         yn (set ays)]
    (if (empty? yn)
      txt
      (recur
       (s/replace txt
                  (join-regex "\\b(" (first yn) ")\\b")
                  (hc/html [:span {:style (str "background-color:" color)} "$1"]))
       (rest yn)))))

(defn exch-html
  "creates the html stock list for an exchange"
  [rows]
  (hc/html [:table {:border 1 :style "text-align:center"}
            (for [row rows]
              (let [sym (:symbol row)
                    com (:company row)
                    yak (:yayk row)
                    nak (:nayk row)
                    mak (:mayk row)
                    des (color-me
                         (color-me
                          (color-me (:desc row)
                                    (:yay row) "lightgreen")
                          (:nay row) "lightpink")
                         (:may row) "lightgrey")
                    url (str "https://finance.yahoo.com/quote/" sym)]
                (conj [:tr
                       [:td [:a {:href url} sym]]
                       [:td com]
                       [:td {:style "background-color:lightgreen"} yak]
                       [:td {:style "background-color:lightpink"} nak]
                       [:td {:style "background-color:lightgrey"} mak]]
                      [:tr [:td {:colspan 5 :style "text-align:justify"} des]])))]))

(defn main
  "given an exchange does all processing and outputs htmlpage"
  [exch]
  (let [exchange-ds (ds-final exch)
        rows-from-ds (ds/mapseq-reader exchange-ds)
        htmlpage (exch-html rows-from-ds)]
    (spit (str "resources/" exch ".html") htmlpage)))


;; mays incorporated
