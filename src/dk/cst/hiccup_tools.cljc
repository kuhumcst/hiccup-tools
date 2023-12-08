(ns dk.cst.hiccup-tools
  "Functions for navigating and structurally transforming Hiccup."
  (:require [clojure.zip :as zip]
            [hickory.zip :as hzip]))

(defn top-loc
  "Find the top-most loc of this `loc` that is not the root."
  [loc]
  (loop [loc loc]
    (let [parent (zip/up loc)]
      ;; The parent is the root node if *its* parent is nil.
      (if (nil? (zip/up parent))
        loc
        (recur parent)))))

(defn node-head
  [[tag attr]]
  (if (map? attr)
    [tag attr]
    [tag]))

(defn insert-front
  [node children]
  (let [head (node-head node)]
    (into head (concat children (subvec node (count head))))))

(defn insert-back
  [node children]
  (into node children))

(defn replace-children
  [node children]
  (into (node-head node) children))

(defn split-node
  "Split the node at `loc` into [left right] nodes.

  The two nodes that are returned will have the HTML tag and attributes of the
  parent node, but different children (i.e. before and after the split).

  The node present at `loc` can optionally be retained at either side of the
  split; however, by default it will be left out."
  [[node :as loc] & {:keys [retain] :as opts}]
  (let [before (zip/lefts loc)
        after  (zip/rights loc)
        [parent] (zip/up loc)]
    (case retain
      :before
      [(replace-children parent (conj before node))
       (replace-children parent after)]

      :after
      [(replace-children parent before)
       (replace-children parent (concat [node] after))]

      ;; do not retain
      [(replace-children parent before)
       (replace-children parent after)])))

(defn split-tree
  "Return [left right] for the structural split at `loc`; the top-level :loc
  of the split is returned as metadata, e.g. for use with zip/replace."
  [[node :as loc] & {:keys [retain] :as opts}]
  (loop [loc   loc
         left  nil
         right nil]
    (let [parent (zip/up loc)]
      ;; The parent is the root node if *its* parent is nil.
      (if (nil? (zip/up parent))
        (with-meta [left right] {:loc loc})
        (let [split  (if (nil? left)                        ; apply opts once
                       (split-node loc opts)
                       (split-node loc))
              left'  (if left
                       (insert-back (first split) [left])
                       (first split))
              right' (if right
                       (insert-front (second split) [right])
                       (second split))]
          (recur parent left' right'))))))

(defn split-hiccup
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
                         (let [[before after :as split] (split-tree loc opts)]
                           (if before
                             (-> (:loc (meta split))
                                 (zip/insert-left before)
                                 (cond->
                                   (= retain :between)
                                   (zip/insert-left node))
                                 (zip/replace after))
                             ;; If splitting node is the very first element,
                             ;; we must ensure that it also respects :retain!
                             (if retain
                               loc
                               (zip/remove loc))))
                         ;; No split, just proceed.
                         loc))))))
(comment
  ;; Structurally split a Hiccup tree at every [:pb] element (4 in total).
  (->> (split-hiccup

         (fn pb? [x]
           (and (vector? x)
                (= :pb (first x))))

         [:root
          [:pb {:id 1}]
          [:a {}
           [:b {}
            [:c {} 1 [:pb {:id 2}] 2]
            2 [:pb {:id 3}] 3]]
          [:d
           3
           [:pb {:id 4}]
           4
           [:e]]]

         :retain :between)

       (clojure.pprint/pprint))

  ;; The resulting Hiccup, ready to be paginated.
  #_[:root
     [:pb {:id 1}]
     [:a {} [:b {} [:c {} 1]]]
     [:pb {:id 2}]
     [:a {} [:b {} [:c {} 2] 2]]
     [:pb {:id 3}]
     [:a {} [:b {} 3]]
     [:d 3]
     [:pb {:id 4}]
     [:d 4 [:e]]]

  #_.)
