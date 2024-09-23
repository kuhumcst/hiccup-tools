(ns dk.cst.hiccup-tools.hiccup
  "Functions for navigating and structurally transforming Hiccup.

  Hiccup elements are matched using the concept of 'matchers', which can be
  either a predicate function testing the Hiccup node itself or plain Clojure
  data which has a convenient behaviour when used as a matcher, e.g. a keyword
  can be used to match HTML tags."
  (:require [clojure.zip :as zip]
            [clojure.string :as str]
            [hickory.zip :as hzip]
            [dk.cst.hiccup-tools.elem :as elem]
            [dk.cst.hiccup-tools.match :as match]
            [dk.cst.hiccup-tools.zip :as z])
  (:refer-clojure :exclude [get]))

(defn cut
  "Cut every node in `hiccup` whenever the `matcher` matches the node.
  The cut nodes are returned as metadata under the :matches key."
  [matcher hiccup]
  (let [pred    (match/matcher matcher)
        matches (atom [])]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (with-meta (zip/root loc) {:matches (not-empty @matches)})
        (recur (zip/next (if (pred node)
                           (do
                             (swap! matches conj node)
                             (zip/remove loc))
                           loc)))))))

(defn split
  "Structurally split `hiccup` whenever the `matcher` matches the node.
  The split proceeds all the way down to the children of the root node.

  The splitting node can be retained by setting the :retain option, e.g.

      :before  - retain the node *in place* on the left side of the split.
      :after   - retain the node *in place* on the right side of the split.
      :between - place the node *between* the two new trees.

  By default, the node will not be retained."
  [matcher hiccup & {:keys [retain] :as opts}]
  (let [pred (match/matcher matcher)]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (zip/root loc)
        (recur (zip/next (if (pred node)
                           (let [[before after :as res] (z/split-tree loc opts)]
                             (if before
                               ;; The usual case (split occurs after some content)
                               (-> (:loc (meta res))
                                   (zip/insert-left before)
                                   (cond->
                                     (= retain :between)
                                     (zip/insert-left node))
                                   (zip/replace after)
                                   ;; To avoid an infinite loop, we must
                                   ;; fast-forward to the inserted node.
                                   (cond->
                                     (= retain :after)
                                     (z/skip-ahead #(= % node))))
                               ;; If the splitting node is the very first element,
                               ;; we must ensure that it also respects :retain!
                               (if retain
                                 loc
                                 (zip/remove loc))))
                           ;; No split, just proceed.
                           loc)))))))

;; TODO: use clojure.walk instead? probably much faster
(defn search
  "Return a mapping from k->matches in `hiccup` for every pred in `k->matcher`."
  [hiccup k->matcher]
  (let [k->pred    (update-vals k->matcher match/matcher)
        k->matches (atom (zipmap (keys k->matcher) (repeat [])))]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (not-empty @k->matches)
        (recur (zip/next (do
                           (doseq [[k pred] k->pred]
                             (when (pred node)
                               (swap! k->matches update k conj node)))
                           loc)))))))

(defn get
  "Get the first occurrence of a node matching `matcher` in `hiccup`."
  [hiccup matcher]
  (some-> (hzip/hiccup-zip hiccup)
          (z/skip-ahead (match/matcher matcher))
          (zip/node)))

;; TODO: proper support for <pre>
(def html-conversion
  {:conversions
   {#{:address :article :aside :blockquote :canvas :div :dl :fieldset
      :figure :footer :h1 :h2 :h3 :h4 :h5 :h6 :header :hr :main :nav
      :noscript :ol :p :pre :section :table :ul}
    z/surround-lb

    #{:br :dd :dt :figcaption :li :tfoot :thead :tr}
    z/append-lb

    :td
    z/insert-space

    ;; Images are replaced with alt text.
    :img
    (fn [[node :as loc]]
      (if-let [alt (:alt (elem/attr node))]
        (zip/insert-right loc (str "\n[image: " (str/trim alt) "]\n"))
        (zip/remove loc)))

    ;; Interactive and non-text element are scrubbed.
    #{:button :form :head :label :input :nav :select :script :video}
    zip/remove


    {:aria-hidden "true"}
    zip/remove}

   :postprocess
   (fn [s]
     (-> s
         (str/trim)
         (str/replace #"s+\n" "\n")
         (str/replace #"\n\s+\n" "\n\n")
         (str/replace #"\n\n+" "\n\n")))})

;; TODO: allow for multiple matches? e.g. to implement markdown
(defn- run-conversions
  "Convert the node at `loc` if any of the preds in `conversions` matches,
  where `conversions` is a map from matching predicate -> conversion fn.

  These convert fns are responsible for appending special fns to create needed
  whitespace when the final text string is produced in the 'text' fn below."
  [[node :as loc] conversions]
  (loop [[[pred convert] & rem] conversions]
    (if (and pred (pred node) convert)
      (convert loc)
      (if rem
        (recur rem)
        loc))))

;; Normal strings are trimmed as inline elements.
;; Added whitespace is kept as is.
(defn- trim-extra
  [s]
  (if (re-matches #"\s+" s)
    s
    (-> s
        (str/replace #"\n|\t" "")
        (str/replace #" +" " "))))

(defn hiccup->text
  "Convert `hiccup` into plain text.

  An element is treated as inline unless a pred in the optional :conversions
  mapping matches it. These predicates test for the existence of certain nodes
  while the convert fns take the loc of the matched node as an argument and
  return a converted loc, e.g. with a whitespace generating fn inserted.

  See 'html-conversion' for an example of how to build a pred->convert mapping."
  [hiccup & [{:keys [conversions postprocess] :as opts}]]
  (let [text-nodes   (atom [])
        conversions' (update-keys conversions match/matcher)]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (->> @text-nodes
             (map trim-extra)
             (apply str)
             (postprocess))
        (recur (zip/next (if (vector? node)
                           (run-conversions loc conversions')
                           (do
                             (when (string? node)
                               (swap! text-nodes conj node))
                             loc))))))))

(comment
  (search
    [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
    {:matches #{:b {:id true}}})
  #_.)
