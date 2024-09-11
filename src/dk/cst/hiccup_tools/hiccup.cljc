(ns dk.cst.hiccup-tools.hiccup
  "Functions for navigating and structurally transforming Hiccup."
  (:require [clojure.zip :as zip]
            [hickory.zip :as hzip]
            [dk.cst.hiccup-tools.zip :refer [split-tree skip-ahead]]))

(defn cut
  "Cut every node in `hiccup` when (pred node) is true for `pred`.
  The cut nodes are returned as metadata under the :matches key."
  [pred hiccup]
  (let [matches (atom [])]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (with-meta (zip/root loc) {:matches (not-empty @matches)})
        (recur (zip/next (if (pred node)
                           (do
                             (swap! matches conj node)
                             (zip/remove loc))
                           loc)))))))

(defn split
  "Structurally split `hiccup` whenever (pred node) is true for `pred`.
  The split proceeds all the way down to the children of the root node.

  The splitting node can be retained by setting the :retain option, e.g.

      :before  - retain the node *in place* on the left side of the split.
      :after   - retain the node *in place* on the right side of the split.
      :between - place the node *between* the two new trees.

  By default, the node will not be retained."
  [pred hiccup & {:keys [retain] :as opts}]
  (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (if (pred node)
                         (let [[before after :as result] (split-tree loc opts)]
                           (if before
                             ;; The usual case (split occurs after some content)
                             (-> (:loc (meta result))
                                 (zip/insert-left before)
                                 (cond->
                                   (= retain :between)
                                   (zip/insert-left node))
                                 (zip/replace after)
                                 ;; To avoid an infinite loop, we must
                                 ;; fast-forward to the inserted node.
                                 (cond->
                                   (= retain :after)
                                   (skip-ahead node)))
                             ;; If the splitting node is the very first element,
                             ;; we must ensure that it also respects :retain!
                             (if retain
                               loc
                               (zip/remove loc))))
                         ;; No split, just proceed.
                         loc))))))

;; TODO: use clojure.walk instead? probably much faster
(defn search
  "Return a mapping from k->matches in `hiccup` for every pred in `k->pred`."
  [hiccup k->pred]
  (let [k->matches (atom (zipmap (keys k->pred) (repeat [])))]
    (loop [[node :as loc] (hzip/hiccup-zip hiccup)]
      (if (zip/end? loc)
        (not-empty @k->matches)
        (recur (zip/next (do
                           (doseq [[k pred] k->pred]
                             (when (pred node)
                               (swap! k->matches update k conj node)))
                           loc)))))))
