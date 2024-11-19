(ns dk.cst.hiccup-tools.helper)

(defn update-kv-keys
  "Apply `f` to the keys of `kvs`.

  This is an alternative to 'update-keys' that allows for generic kvs as input."
  [kvs f]
  (into (empty kvs)
        (for [[k v] kvs]
          [(f k) v])))

(defn update-kv-vals
  "Apply `f` to the vals of `kvs`.

  This is an alternative to 'update-vals' that allows for generic kvs as input."
  [kvs f]
  (into (empty kvs)
        (for [[k v] kvs]
          [k (f v)])))

(defn prefix-kw
  [prefix kw]
  (keyword (str prefix "-" (name kw))))
