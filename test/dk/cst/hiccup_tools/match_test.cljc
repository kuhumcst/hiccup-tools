(ns dk.cst.hiccup-tools.match-test
  (:require [clojure.test :refer [deftest is testing]]
            [dk.cst.hiccup-tools.match :as match]))

(deftest tag-test
  (let [div-pred (match/tag :div)]
    (testing "any vector beginning with a :div keyword should match"
      (is (div-pred [:div]))
      (is (div-pred [:div {}]))
      (is (div-pred [:div {:id "thing"}]))
      (is (div-pred [:div {:id "thing"} [:span "child"]])))
    (testing "other vectors should not match"
      (is (not (div-pred [:a])))
      (is (not (div-pred [:a {}])))
      (is (not (div-pred [:a {:id "thing"}])))
      (is (not (div-pred [:a {:id "thing"} [:span "child"]]))))))

(deftest empty-attr-test
  (let [empty-pred (match/attr {})
        pos-pred   (match/attr {:id true})
        neg-pred   (match/attr {:id false})
        val-pred   (match/attr {:id "thing"})
        combo-pred (match/attr {:id    false
                                :class true
                                :key   "thing"})]
    (testing "empty attr should match any vector"
      (is (empty-pred [:div]))
      (is (empty-pred [:div {}]))
      (is (empty-pred [:div {:id "thing"}]))
      (is (empty-pred [:div {:id "thing"} [:span "child"]])))
    (testing "positive attr values should match the existence of a key"
      (is (not (pos-pred [:div])))
      (is (not (pos-pred [:div {}])))
      (is (pos-pred [:div {:id "thing"}]))
      (is (pos-pred [:div {:id "thing"} [:span "child"]])))
    (testing "negative attr values should match the absence of a key"
      (is (neg-pred [:div]))
      (is (neg-pred [:div {}]))
      (is (not (neg-pred [:div {:id "thing"}])))
      (is (not (neg-pred [:div {:id "thing"} [:span "child"]]))))
    (testing "specific attr values should match the value of a key"
      (is (not (val-pred [:div])))
      (is (not (val-pred [:div {}])))
      (is (val-pred [:div {:id "thing"}]))
      (is (not (val-pred [:div {:id "not-thing"}])))
      (is (val-pred [:div {:id "thing"} [:span "child"]]))
      (is (not (val-pred [:div {:id "not-thing"} [:span "child"]]))))
    (testing "attr combinations should match every key"
      (is (combo-pred [:div {:class "present"
                             :key   "thing"}]))
      (is (not (combo-pred [:div {:id    "present"
                                  :class "present"
                                  :key   "thing"}])))
      (is (not (combo-pred [:div {:key "thing"}])))
      (is (not (combo-pred [:div {:class "present"
                                  :key   "other"}]))))))

(deftest tag+attr-test
  (let [pred (match/tag+attr :span {:id    true
                                    :class "thing"})]
    (testing "the combination of tag+attr should match both"
      (is (pred [:span {:id    "present"
                        :class "thing"}
                 "content"]))
      (is (not (pred [:div {:id    "present"
                            :class "thing"}
                      "content"])))
      (is (not (pred [:span {:class "thing"}
                      "content"]))))))

(deftest hiccup-test
  (let [pred (match/hiccup [:span {:id    true
                                   :class "thing"}])]
    (testing "the destructured Hiccup combination of tag+attr should match both"
      (is (pred [:span {:id    "present"
                        :class "thing"}
                 "content"]))
      (is (not (pred [:div {:id    "present"
                            :class "thing"}
                      "content"])))
      (is (not (pred [:span {:class "thing"}
                      "content"]))))))
