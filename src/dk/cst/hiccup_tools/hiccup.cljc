(ns dk.cst.hiccup-tools.hiccup
  "Functions for navigating and structurally transforming Hiccup.

  Hiccup elements are matched using the concept of 'matchers', which can be
  either a predicate function testing the Hiccup node itself or plain Clojure
  data which has a convenient behaviour when used as a matcher, e.g. a keyword
  can be used to match HTML tags."
  (:require [clojure.zip :as zip]
            [clojure.string :as str]
            [hickory.zip :as hzip]
            [dk.cst.hiccup-tools.helper :as helper]
            [dk.cst.hiccup-tools.elem :as elem]
            [dk.cst.hiccup-tools.match :as match]
            [dk.cst.hiccup-tools.zip :as z])
  (:refer-clojure :exclude [get]))

(defn cut
  "Cut every node in `hiccup` whenever the `matcher` matches the node at loc.
  The cut nodes are returned as metadata under the :matches key."
  [matcher hiccup]
  (let [pred    (match/match matcher)
        matches (atom [])]
    (loop [loc (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (with-meta (zip/root loc) {:matches (not-empty @matches)})
        (recur (zip/next (if (pred loc)
                           (do
                             (swap! matches conj (zip/node loc))
                             (zip/remove loc))
                           loc)))))))

(defn split
  "Structurally split `hiccup` whenever the `matcher` matches the node at loc.
  The split proceeds all the way down to the children of the root node.

  The splitting node can be retained by setting the :retain option, e.g.

      :before  - retain the node *in place* on the left side of the split.
      :after   - retain the node *in place* on the right side of the split.
      :between - place the node *between* the two new trees.

  By default, the node will not be retained."
  [matcher hiccup & {:keys [retain] :as opts}]
  (let [pred (match/match matcher)]
    (loop [loc (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (zip/root loc)
        (recur (zip/next (if (pred loc)
                           (let [[before after :as res] (z/split-tree loc opts)]
                             (if before
                               ;; The usual case (split occurs after some content)
                               (-> (:loc (meta res))
                                   (zip/insert-left before)
                                   (cond->
                                     (= retain :between)
                                     (zip/insert-left (zip/node loc)))
                                   (zip/replace after)
                                   ;; To avoid an infinite loop, we must
                                   ;; fast-forward to the inserted node.
                                   (cond->
                                     (= retain :after)
                                     (z/skip-ahead (let [node (zip/node loc)]
                                                     (fn [candidate]
                                                       (= (zip/node candidate)
                                                          node))))))
                               ;; If the splitting node is the very first element,
                               ;; we must ensure that it also respects :retain!
                               (if retain
                                 loc
                                 (zip/remove loc))))
                           ;; No split, just proceed.
                           loc)))))))

(defn search
  "Return a mapping from k->matches in `hiccup` for every pred in `k->matcher`.

  There is an optional :on-match parameter for the search behaviour. By default,
  matched nodes AND their children are skipped (:skip-tree). You may specify
  :continue if the search should continue within matched nodes too or :skip-node
  if only a single matcher should apply to a matched node.

  The :on-match option can also be specified via metadata of both the
  `k->matcher` and the matcher."
  [hiccup k->matcher & {:keys [on-match] :as opts
                        :or   {on-match :skip-tree}}]
  (let [k->pred    (helper/update-kv-vals k->matcher match/match)
        k->matches (atom (zipmap (map first k->matcher) (repeat [])))
        type       (or (:on-match (meta k->matcher)) on-match)]
    (loop [loc (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (->> (remove (comp empty? second) @k->matches)
             (into {})
             (not-empty))
        (recur (zip/next (loop [[[k pred] & k->pred'] k->pred]
                           (cond
                             (and k pred (pred loc))
                             (do
                               (swap! k->matches update k conj (zip/node loc))
                               (condp = (or (:on-match (meta pred)) type)
                                 ;; :continue searches will exhaustively try
                                 ;; every matcher on every node in the zipper.
                                 :continue
                                 (recur k->pred')

                                 ;; :skip-tree searches remove matches from the
                                 ;; tree, though we might simulate an early end
                                 ;; to avoid potentially removing the root node.
                                 :skip-tree
                                 (if (nil? (zip/path loc))
                                   [(zip/node loc) :end]
                                   (zip/remove loc))

                                 ;; :skip-node searches just return the current
                                 ;; loc, ending the node's matching loop.
                                 :skip-node
                                 loc))

                             (not-empty k->pred')
                             (recur k->pred')

                             :else loc))))))))

(defn get
  "Get the first occurrence of a node matching `matcher` in `hiccup`."
  [hiccup matcher]
  (some-> (hzip/hiccup-zip hiccup)
          (z/skip-ahead (match/match matcher))
          (zip/node)))

;; TODO: proper support for <pre>
(def html-text
  {:single
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

(defn run-conversions
  "Convert the node at `loc` if any of the preds in `conversions` matches,
  where `conversions` is a map from matching predicate -> conversion fn.

  These convert fns are responsible for appending special fns to create needed
  whitespace when the final text string is produced in the 'text' fn below.

  The option :on-match may be set to :continue if the (otherwise default)
  behaviour of an early exit after each match shouldn't be observed."
  [loc conversions & {:keys [on-match]}]
  (loop [[[pred convert] & rem] conversions
         loc' loc]
    (if (and pred (pred loc') convert)
      (if (and (= on-match :continue)
               (not (empty? rem)))
        (recur rem (convert loc'))
        (convert loc'))
      (if rem
        (recur rem loc')
        loc'))))

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
  "Convert a `hiccup` tree into plain text.

  An element is treated as inline unless a pred in the optional conversions
  mapping matches it. These predicates test for the existence of certain nodes
  while the convert fns take the loc of the matched node as an argument and
  return a converted loc, e.g. with a whitespace generating fn inserted.

  Conversions listed under the :single option exit after the first match, while
  conversions listed under the :multi option exhaust every possible conversion.

  See 'html-conversion' for an example of how to build a pred->convert mapping."
  [hiccup & [{:keys [single multi preprocess postprocess] :as opts
              :or   {single      []
                     multi       []
                     preprocess  identity
                     postprocess identity}}]]
  (let [text-nodes (atom [])
        single'    (helper/update-kv-keys single match/match)
        multi'     (helper/update-kv-keys multi match/match)]
    (loop [loc (preprocess (hzip/hiccup-zip hiccup))]
      (if (zip/end? loc)
        (->> @text-nodes
             (map trim-extra)
             (apply str)
             (postprocess))
        (recur (zip/next (if (zip/branch? loc)
                           (-> loc
                               (run-conversions single' :on-match :stop)
                               (run-conversions multi' :on-match :continue))
                           (let [node (zip/node loc)]
                             (when (string? node)
                               (swap! text-nodes conj node))
                             loc))))))))

(def valid-html
  "Conversions for turning XML-based Hiccup into valid HTML."
  {:single {(match/any) z/html-safe}})

(defn reshape
  "Reshape a `hiccup` tree.

  An element is treated as inline unless a pred in the optional conversions
  mapping matches it. These predicates test for the existence of certain nodes
  while the convert fns take the loc of the matched node as an argument and
  return a converted loc, e.g. with a whitespace generating fn inserted.

  Conversions listed under the :single option exit after the first match, while
  conversions listed under the :multi option exhaust every possible conversion.

  See 'valid-html' for an example of how to build a pred->convert mapping."
  [hiccup & [{:keys [single multi preprocess postprocess] :as opts
              :or   {single      []
                     multi       []
                     preprocess  identity
                     postprocess identity}}]]
  (let [single' (helper/update-kv-keys single match/match)
        multi'  (helper/update-kv-keys multi match/match)]
    (loop [loc (preprocess (hzip/hiccup-zip hiccup))]
      (if (zip/end? loc)
        (postprocess (zip/root loc))
        (recur (zip/next (if (zip/branch? loc)
                           (-> loc
                               (run-conversions single' :on-match :stop)
                               (run-conversions multi' :on-match :continue))
                           loc)))))))

(comment
  (reshape [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
           valid-html)

  (reshape [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
           {:single {:a          identity
                     (match/any) z/html-safe}
            :multi  {:x-c            (fn [loc] (zip/append-child loc 123))
                     {:data-id true} (fn [loc] (zip/append-child loc 456))}})

  (search
    [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
    {:matches #{:b {:id true}}})

  ;; kvs test
  (search
    [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
    [[:matches #{:b {:id true}}]])
  #_.)
