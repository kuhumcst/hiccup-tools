(ns dk.cst.hiccup-tools.match-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.zip :as zip]
            [hickory.zip :refer [hiccup-zip]]
            [dk.cst.hiccup-tools.match :as match]))

;; Helper function so that all matchers are fed zipper locs
(defn comp-zip
  [matcher]
  (comp matcher hiccup-zip))

(deftest tag-test
  (let [div-pred (comp-zip (match/tag :div))]
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

(deftest tags-test
  (let [pred (comp-zip (match/tags :div :span))]
    (testing "any vector beginning with a :div keyword should match"
      (is (pred [:div]))
      (is (pred [:div {}]))
      (is (pred [:div {:id "thing"}]))
      (is (pred [:div {:id "thing"} [:span "child"]])))
    (testing "any vector beginning with a :div keyword should match"
      (is (pred [:span]))
      (is (pred [:span {}]))
      (is (pred [:span {:id "thing"}]))
      (is (pred [:span {:id "thing"} [:span "child"]])))
    (testing "other vectors should not match"
      (is (not (pred [:a])))
      (is (not (pred [:a {}])))
      (is (not (pred [:a {:id "thing"}])))
      (is (not (pred [:a {:id "thing"} [:span "child"]]))))))

(deftest attr-test
  (let [empty-pred (comp-zip (match/attr {}))
        pos-pred   (comp-zip (match/attr {:id true}))
        neg-pred   (comp-zip (match/attr {:id false}))
        val-pred   (comp-zip (match/attr {:id "thing"}))
        combo-pred (comp-zip (match/attr {:id    false
                                          :class true
                                          :key   "thing"}))]
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
  (let [pred (comp-zip (match/tag+attr :span {:id    true
                                              :class "thing"}))]
    (testing "the combination of tag+attr should match both"
      (is (pred [:span {:id    "present"
                        :class "thing"}
                 "content"]))
      (is (not (pred [:div {:id    "present"
                            :class "thing"}
                      "content"])))
      (is (not (pred [:span {:class "thing"}
                      "content"]))))))

(deftest has-child-test
  (let [pred       (comp-zip (match/has-child (match/tag+attr :span {:id    true
                                                                     :class "thing"})))
        index-pred (comp-zip (match/has-child (match/tag+attr :span {:id    true
                                                                     :class "thing"})
                                              1))]
    (testing "should only match direct children"
      (is (pred [:div
                 [:span {:id    "present"
                         :class "thing"}
                  "content"]]))
      (is (not (pred [:span {:id    "present"
                             :class "thing"}
                      "content"])))
      (is (not (pred [:span {:id    true
                             :class "thing"}
                      [:div
                       [:span {:id    "present"
                               :class "thing"}
                        "content"]]]))))
    (testing "number or order of children should not matter"
      (is (pred [:div
                 [:div "other content"]
                 [:span {:id    "present"
                         :class "thing"}
                  "content"]
                 [:span "other content"]])))
    (testing "a 'matching' parent should not make a difference"
      (is (pred [:span {:id    true
                        :class "thing"}
                 [:span {:id    "present"
                         :class "thing"}
                  "content"]])))
    (testing "the index should limit to a specific position"
      (is (index-pred [:div
                       [:div]
                       [:span {:id    "present"
                               :class "thing"}
                        "content"]]))
      (is (not (index-pred [:div
                            [:span {:id    "present"
                                    :class "thing"}
                             "content"]
                            [:div]]))))))

(deftest has-parent-test
  (let [skip (fn [n hiccup]
               (->> (hiccup-zip hiccup)
                    (iterate zip/next)
                    (take (inc n))
                    (last)))
        pred (match/has-parent (match/tag+attr :div {:id    true
                                                     :class "thing"}))]
    (testing "should only match direct ancestor"
      (is (pred (skip 1 [:div {:id    "present"
                               :class "thing"}
                         [:span
                          "content"]])))
      (is (pred (skip 2 [:div
                         [:div {:id    "present"
                                :class "thing"}
                          [:span
                           "content"]]])))
      (is (not (pred (skip 0 [:div {:id    "present"
                                    :class "thing"}]))))
      (is (not (pred (skip 2 [:div {:id    "present"
                                    :class "thing"}
                              [:div
                               [:span
                                "content"]]])))))
    (testing "number or order of children should not matter"
      (is (pred (skip 2 [:div {:id    "present"
                               :class "thing"}
                         [:span]
                         [:span]
                         [:span]]))))))

(deftest hiccup-test
  (let [pred (comp-zip (match/hiccup [:span {:id    true
                                             :class "thing"}]))]
    (testing "the destructured Hiccup combination of tag+attr should match both"
      (is (pred [:span {:id    "present"
                        :class "thing"}
                 "content"]))
      (is (not (pred [:div {:id    "present"
                            :class "thing"}
                      "content"])))
      (is (not (pred [:span {:class "thing"}
                      "content"]))))))

(deftest matcher-test
  (testing "these data types should be explicitly supported"
    (is (fn? (match/match (fn []))))
    (is (fn? (match/match {})))
    (is (fn? (match/match #{})))
    (is (fn? (match/match [:div {}])))
    (is (fn? (match/match :div))))
  (testing "these data types should not be allowed"
    (is (thrown? Exception (fn? (match/match nil))))
    (is (thrown? Exception (fn? (match/match "string"))))
    (is (thrown? Exception (fn? (match/match 123))))
    (is (thrown? Exception (fn? (match/match '())))))
  (let [fn-pred        (comp-zip (match/match (fn [comp-zip]
                                                (and (zip/branch? comp-zip)
                                                     (map? (second (zip/node comp-zip)))))))
        tag-pred       (comp-zip (match/match :div))
        attr-pred      (comp-zip (match/match {:class "something"}))
        hiccup-pred    (comp-zip (match/match [:div {:class "something"}]))
        set-pred       (comp-zip (match/match #{:div :span {:id true}}))
        empty-set-pred (comp-zip (match/match #{}))
        combo-pred     (comp-zip (match/match :a {:id true} {:class true}))]
    (testing "basic data types should result in basic matchers"
      (is (fn-pred [:div {:class "something"}]))
      (is (not (fn-pred [:div])))
      (is (tag-pred [:div {:class "something"}]))
      (is (not (tag-pred [:span {:class "something"}])))
      (is (attr-pred [:div {:class "something"}]))
      (is (attr-pred [:span {:class "something"}])))
    (testing "the hiccup matcher should match Hiccup loosely, not just exact"
      (is (hiccup-pred [:div {:class "something"}]))
      (is (hiccup-pred [:div {:class "something"} "child"]))
      (is (not (hiccup-pred [:div {}])))
      (is (not (hiccup-pred [:span {} [:div {:class "something"}]]))))
    (testing "sets should expand into a union of matchers"
      (is (set-pred [:div {} "glen"]))
      (is (set-pred [:span]))
      (is (set-pred [:a {:id "glen" :class "something"}]))
      (is (not (set-pred [:a {:class "something"}]))))
    (testing "empty sets should match nothing"
      (is (not (empty-set-pred [:div {} "glen"])))
      (is (not (empty-set-pred [:span])))
      (is (not (empty-set-pred [:a {:id "glen" :class "something"}])))
      (is (not (empty-set-pred [:a {:class "something"}]))))
    (testing "matcher combinations must match every pred"
      (is (combo-pred [:a {:id "glen" :class "something"} "child"]))
      (is (not (combo-pred [:a {:id "glen"}])))
      (is (not (combo-pred [:a]))))))
