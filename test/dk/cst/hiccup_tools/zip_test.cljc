(ns dk.cst.hiccup-tools.zip-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.zip :as zip]
            [hickory.zip :as hzip]
            [dk.cst.hiccup-tools.zip :as z]
            [dk.cst.hiccup-tools.match :as match]))

(deftest skip-ahead-test
  (let [loc (hzip/hiccup-zip [:root
                              [:pb {:id 1}]
                              [:a {}
                               [:b {}
                                [:c {} 1 [:pb {:id 2}] 2]
                                2 [:pb {:id 3}] 3]]
                              [:d
                               3
                               [:pb {:id 4 :class "thing"}]
                               4
                               [:e]]])]
    (testing "skipping ahead should move loc forward to specified node"
      (is (= (zip/node (z/skip-ahead loc (match/match [:pb {:id 4 :class "thing"}])))
             [:pb {:id 4 :class "thing"}])))
    (testing "skipping ahead should move loc forward to node matching pred"
      (is (= (zip/node (z/skip-ahead loc (match/attr {:class "thing"})))
             [:pb {:id 4 :class "thing"}])))))

(deftest top-level-test
  (let [loc (hzip/hiccup-zip [:root
                              [:pb {:id 1}]
                              [:a {}
                               [:b {}
                                [:c {} 1 [:pb {:id 2}] 2]
                                2 [:pb {:id 3}] 3]]
                              [:d
                               3
                               [:pb {:id 4 :class "thing"}]
                               4
                               [:e]]])]
    (testing "the node should be the node of the final top loc"
      (is (= (-> (z/skip-ahead loc (match/match [:pb {:id 4 :class "thing"}]))
                 (z/top-level)
                 (zip/node))
             [:d
              3
              [:pb {:id 4 :class "thing"}]
              4
              [:e]])))))

(deftest splice-test
  (let [loc (hzip/hiccup-zip [:root
                              [:node 1 2 3 [:node 4 5 6]]
                              [:node 7 8 9]])]
    (testing "splice should not skip the spliced in children"
      ;; Since we don't call next the last loc is returned, i.e. the root.
      (is (= (-> loc zip/next z/splice zip/node)
             (-> loc zip/next z/splice zip/root)
             [:root
              1 2 3 [:node 4 5 6]                           ; spliced nodes
              [:node 7 8 9]])))
    (testing "splice and splice-skip do the same transformation"
      ;; Since we don't call zip/next and splice removes the node, the last loc
      ;; in the D.F.S. is returned, i.e. the now transformed root node.
      (is (= (-> loc zip/next z/splice zip/root)
             (-> loc zip/next z/splice-skip zip/root)
             [:root
              1 2 3 [:node 4 5 6]                           ; spliced nodes
              [:node 7 8 9]])))
    (testing "splice-skip should skip the spliced in children"
      ;; Since we don't call zip/next, the last loc in the D.F.S. is returned,
      ;; i.e. 6, even though we never actually visited this node!
      (is (= (-> loc zip/next z/splice-skip zip/node)
             6))
      ;; The next node in the D.F.S. is still what we would expect, however.
      (is (= (-> loc zip/next z/splice-skip zip/next zip/node)
             [:node 7 8 9])))
    (testing "splice and splice-skip also accept a coll replacing the children"
      (is (= (-> loc zip/next (z/splice ["a" "b" "c"]) zip/root)
             (-> loc zip/next (z/splice-skip ["a" "b" "c"]) zip/root)
             [:root
              "a" "b" "c"                                   ; spliced nodes
              [:node 7 8 9]])))))

