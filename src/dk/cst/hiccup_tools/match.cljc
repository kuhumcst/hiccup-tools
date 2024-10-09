(ns dk.cst.hiccup-tools.match
  "Predicates for matching against Hiccup vector locs in a zipper."
  (:require [dk.cst.hiccup-tools.elem :as elem]
            [hickory.zip :as hzip]
            [clojure.zip :as zip]))

;; helper function
(defn- loc->node
  [loc]
  (when (zip/branch? loc)
    (zip/node loc)))

(defn tag
  "Get a predicate for matching elements with the tag `k`."
  [k]
  (assert (keyword? k))
  (fn [loc]
    (when-let [[tag] (loc->node loc)]
      (= tag k))))

(defn tags
  "Get a predicate for matching elements with the tags in `kset`."
  [& ks]
  (assert (every? keyword? ks))
  (let [kset (set ks)]
    (fn [loc]
      (when-let [node (loc->node loc)]
        (get kset (first node))))))

(defn attr
  "Get a predicate for matching elements with attribute maps matching `m`.

  A value of true in `m` can be used to match on just the existence of the key,
  whilst a value of nil/false can be used to match on the absence of the key.
  Any other values must match *directly* with the values of the attr map."
  [m]
  (assert (map? m))
  (fn [loc]
    (when-let [node (loc->node loc)]
      (let [attr (elem/attr node)]
        (loop [[[k v] & m'] m]
          (if (nil? k)
            true
            (if (contains? attr k)
              (when (or (true? v)
                        (= (get attr k) v))
                (recur m'))
              (when (not v)
                (recur m')))))))))

(defn has-child
  "Get a predicate matching elements where `pred` is true for at least one of
  the children. If `index` is supplied, the child must match the exact position.

  This can compose with other predicates, e.g. those from this namespace:

    (child (attr {:class \"label\"}))

  The above matches the parent element of a child with the :class `label`."
  ([pred]
   (assert (fn? pred))
   (fn [loc]
     (when-let [parent (loc->node loc)]
       (loop [[child & children] (zip/children loc)]
         (cond
           (and (vector? child)
                (pred (hzip/hickory-zip child))) parent
           children (recur children))))))
  ([pred index]
   (assert (fn? pred))
   (assert (number? index))
   (fn [loc]
     (when-let [parent (loc->node loc)]
       (let [child (nth (elem/children parent) index)]
         (when (and (vector? child)
                    (pred (hzip/hickory-zip child)))
           parent))))))

(defn has-parent
  "Get a predicate matching elements where `pred` is true for its parent.

  This can compose with other predicates, e.g. those from this namespace:

    (parent (attr {:class \"label\"}))

  The above matches the child element of the parent with the :class `label`."
  [pred]
  (assert (fn? pred))
  (fn [loc]
    (some-> loc zip/up pred)))

(defn tag+attr
  "Get a predicate for matching both tag `k` and attr `m`."
  [k m]
  (every-pred (tag k) (attr m)))

;; TODO: expand to include some element of child matching too
(defn hiccup
  "Get a predicate for matching both tag `k` and attr `m` in a hiccup vector."
  [[k m]]
  (tag+attr k m))

(declare match)

(defn any
  "Get a predicate that will match any of the provided compatible data in `xs`."
  [& xs]
  (if (empty? xs)
    (constantly false)
    (let [tags  (when-let [ks (not-empty (filter keyword? xs))]
                  (apply tags ks))
          other (map match (remove keyword? xs))]
      (if tags
        (apply some-fn tags other)
        (apply some-fn other)))))

(defn match
  "Get a predicate to match Hiccup locs based on a piece of compatible data `x`.

  If multiple `xs` are given as arguments, a combined matcher is returned which
  must satisfy all these aspects simultaneously.

  The following data types are supported for matchers:

    fn       - used directly as a predicate function
    keyword  - matches the tag
    map      - matches the attributes
    set      - the union of matchers constituted by the items in the set
    other    - matches the exact data provided"
  ([x]
   (cond
     (fn? x) x
     (keyword? x) (tag x)
     (set? x) (apply any x)
     (map? x) (attr x)
     (vector? x) (hiccup x)
     :else (throw (ex-info "unsupported type of matcher:" {:input x
                                                           :type  (type x)}))))
  ([x & xs]
   (apply every-pred (match x) (map match xs))))
