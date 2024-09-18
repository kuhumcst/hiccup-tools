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
      (is (= (zip/node (z/skip-ahead loc (match/matcher [:pb {:id 4 :class "thing"}])))
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
      (is (= (-> (z/skip-ahead loc (match/matcher [:pb {:id 4 :class "thing"}]))
                 (z/top-level)
                 (zip/node))
             [:d
              3
              [:pb {:id 4 :class "thing"}]
              4
              [:e]])))))
