(ns dk.cst.hiccup-tools.elem
  "Basic operations on Hiccup vectors; no dependency on clojure.zip.")

(defn attr
  [[tag attr]]
  (if (map? attr)
    attr
    {}))

(defn head
  [[tag attr]]
  (if (map? attr)
    [tag attr]
    [tag]))

(defn children
  [[tag & children]]
  (if (map? (first children))
    (rest children)
    children))

(defn insert-front
  [node children]
  (let [head (head node)]
    (into head (concat children (subvec node (count head))))))

(defn insert-back
  [node children]
  (into node children))

(defn replace-children
  [node children]
  (into (head node) children))

(comment
  (head [:p 1 2 3])
  (head [:p {:id "glen"} 1 2 3])
  (children [:p {:id "glen"} 1 2 3])
  #_.)
