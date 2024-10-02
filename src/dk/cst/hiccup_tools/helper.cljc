(ns dk.cst.hiccup-tools.helper)

(defn update-kv-vals
  "Apply `f` to the vals of `kvs`. This is an alternative to 'update-vals' that
  allows for kvs to be used in place of maps, e.g. when order is important."
  [kvs f]
  (into (empty kvs)
        (for [[k v] kvs]
          [k (f v)])))
