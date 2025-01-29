(ns dk.cst.hiccup-tools.zip
  "Functions for navigating and structurally transforming zippers."
  (:require [clojure.zip :as zip]
            [dk.cst.hiccup-tools.helper :as helper]
            [dk.cst.hiccup-tools.elem :as elem]))

(defn top-level
  "Find the top-most loc of this `loc` that is not the root."
  [loc]
  (loop [loc loc]
    (let [parent (zip/up loc)]
      ;; The parent is the root node if *its* parent is nil.
      (if (nil? (zip/up parent))
        loc
        (recur parent)))))

(defn skip-ahead
  "Fast-forward a zipper at `loc` to the loc where `pred` matches the node."
  [loc pred]
  (loop [loc' loc]
    (cond
      (zip/end? loc') nil
      (pred loc') loc'
      :else (recur (zip/next loc')))))

(defn splice-skip
  "Replace a `loc` with its children (default behaviour) and skip them;
  or, optionally, supply a coll of `nodes` to replace the loc with instead."
  ([loc]
   (splice-skip loc (zip/children loc)))
  ([loc nodes]
   ;; Applying insert-left to the list of nodes ensures that we will *not*
   ;; visit any of these nodes when calling zip/next on the returned loc.
   (->> nodes
        (reduce (fn [loc child]
                  (zip/insert-left loc child))
                loc)
        (zip/remove))))

(defn splice
  "Replace a `loc` with its children (default behaviour), but do not skip them;
  or, optionally, supply a coll of `nodes` to replace the loc with instead."
  ([loc]
   (splice loc (zip/children loc)))
  ([loc nodes]
   ;; Applying insert-right to the reversed list of nodes rather than applying
   ;; insert-left to the actual list ensures that these children *are* visited.
   (->> (reverse nodes)
        (reduce (fn [loc child]
                  (zip/insert-right loc child))
                loc)
        (zip/remove))))

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
      [(elem/replace-children parent (concat before [node]))
       (elem/replace-children parent after)]

      :after
      [(elem/replace-children parent before)
       (elem/replace-children parent (concat [node] after))]

      ;; do not retain
      [(elem/replace-children parent before)
       (elem/replace-children parent after)])))

(defn split-tree
  "Return [left right] for the structural split at `loc`; the top-level :loc
  of the split is returned as metadata, e.g. for use with zip/replace."
  [loc & {:keys [retain] :as opts}]
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
                       (elem/insert-back (first split) [left])
                       (first split))
              right' (if right
                       (elem/insert-front (second split) [right])
                       (second split))]
          (recur parent left' right'))))))

(defn surround-lb
  "Insert linebreaks at either end of `loc`."
  [loc]
  (-> loc
      (zip/insert-child "\n")
      (zip/append-child "\n")))

(defn append-lb
  "Insert a single linebreak at the end of `loc`."
  [loc]
  (zip/append-child loc "\n"))

(defn insert-space
  "Insert a space at the end of `loc`."
  [loc]
  (zip/append-child loc " "))

(def ^:dynamic *custom-element-prefix* "x")

(defn html-safe
  "Make the node at `loc` HTML-safe."
  [loc]
  (let [[tag attr children] (elem/parts (zip/node loc))
        prefix-data (fn [kw] (helper/prefix-kw "data" kw))
        tag'        (helper/prefix-kw *custom-element-prefix* tag)
        attr'       (update-keys attr prefix-data)]
    (zip/replace loc (into [tag' attr'] children))))
