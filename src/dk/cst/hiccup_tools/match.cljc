(ns dk.cst.hiccup-tools.match
  "Predicates for matching against Hiccup vectors."
  (:require [dk.cst.hiccup-tools.elem :as elem]))

(defn tag
  "Get a predicate for matching elements with the tag `k`."
  [k]
  #(and (vector? %)
        (= (first %) k)))

(defn tags
  "Get a predicate for matching elements with the tags in `kset`."
  [& ks]
  (let [kset (set ks)]
    #(and (vector? %)
          (get kset (first %)))))

(defn attr
  "Get a predicate for matching elements with attribute maps matching `m`.

  A value of true in `m` can be used to match on just the existence of the key,
  whilst a value of nil/false can be used to match on the absence of the key.
  Any other values must match *directly* with the values of the attr map."
  [m]
  #(when (vector? %)
     (let [attr (if (map? (second %))
                  (second %)
                  {})]
       (loop [[[k v] & m'] m]
         (if (nil? k)
           true
           (if (contains? attr k)
             (when (or (true? v)
                       (= (get attr k) v))
               (recur m'))
             (when (not v)
               (recur m'))))))))

(defn child
  "Get a predicate matching elements where `pred` is true for at least one of
  the children. If `index` is supplied, the child must match the exact position.

  This can compose with other predicates, e.g. those from this namespace:

    (child (attr {:class \"label\"}))

  The above matches an element containing a child with the :class `label`."
  ([pred]
   #(when (vector? %)
      (loop [[node & nodes] (elem/children %)]
        (cond
          (pred node) node
          nodes (recur nodes)))))
  ([pred index]
   #(when (vector? %)
      (let [child (nth (elem/children %) index)]
        (when (pred child)
          child)))))

(defn tag+attr
  "Get a predicate for matching both tag `k` and attr `m`."
  [k m]
  (every-pred (tag k) (attr m)))

(defn hiccup
  "Get a predicate for matching both tag `k` and attr `m` in a hiccup vector."
  [[k m]]
  (tag+attr k m))