(ns dk.cst.hiccup-tools.match
  "Predicates for matching against Hiccup vectors.")

(defn tag
  "Get a predicate for matching elements with the tag `k`."
  [k]
  #(and (vector? %)
        (= (first %) k)))

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

(defn tag+attr
  "Get a predicate for matching both tag `k` and attr `m`."
  [k m]
  (every-pred (tag k) (attr m)))

(defn hiccup
  "Get a predicate for matching both tag `k` and attr `m` in a hiccup vector."
  [[k m]]
  (tag+attr k m))
