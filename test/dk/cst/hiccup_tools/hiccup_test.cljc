(ns dk.cst.hiccup-tools.hiccup-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.zip :as zip]
            [dk.cst.hiccup-tools.zip :as z]
            [dk.cst.hiccup-tools.example :as example]
            [dk.cst.hiccup-tools.hiccup :as h]
            [dk.cst.hiccup-tools.match :as match]))

(deftest cut-test
  (let [doc [:root
             [:pb {:id 1}]
             [:a {}
              [:b {}
               [:c {} 1 [:pb {:id 2}] 2]
               2 [:pb {:id 3}] 3]]
             [:d
              3
              [:pb {:id 4 :class "thing"}]
              4
              [:e]]]]
    (let [result (h/cut (match/tag :pb) doc)]
      (testing "every :pb element should be cut from the doc"
        (is (= result
               [:root
                [:a {}
                 [:b {}
                  [:c {} 1 2] 2 3]]
                [:d 3 4
                 [:e]]])))
      (testing "every :pb element should be present as a match"
        (is (= (:matches (meta result))
               [[:pb {:id 1}]
                [:pb {:id 2}]
                [:pb {:id 3}]
                [:pb {:id 4, :class "thing"}]]))))))

(deftest search-test
  (let [doc [:root
             [:pb {:id 1}]
             [:a {:id 2}
              [:b {}
               [:c {:id 3} 1 [:pb {:id 4}] 2]
               2 [:pb] 3]]
             [:d
              3
              [:pb {:id 5 :class "thing"}]
              4
              [:e]]]]
    (testing ":exhaustive searches should find every matchers match"
      (is (= (h/search doc {:pb    (match/hiccup [:pb {:id    true
                                                       :class false}])
                            :id    (match/attr {:id true})
                            :no-id (match/attr {:id false})}
                       :on-match :continue)
             {:pb    [[:pb {:id 1}] [:pb {:id 4}]],
              :id    [[:pb {:id 1}]
                      [:a {:id 2} [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]]
                      [:c {:id 3} 1 [:pb {:id 4}] 2]
                      [:pb {:id 4}]
                      [:pb {:id 5, :class "thing"}]],
              :no-id [[:root
                       [:pb {:id 1}]
                       [:a {:id 2} [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]]
                       [:d 3 [:pb {:id 5, :class "thing"}] 4 [:e]]]
                      [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]
                      [:pb]
                      [:d 3 [:pb {:id 5, :class "thing"}] 4 [:e]]
                      [:e]]})))
    (testing ":skip-node searches should find every matching element only once"
      (is (= (h/search doc [[:pb (match/hiccup [:pb {:id    true
                                                     :class false}])]
                            [:id (match/attr {:id true})]
                            [:no-id (match/attr {:id false})]]
                       :on-match :skip-node)
             {:pb    [[:pb {:id 1}] [:pb {:id 4}]],
              :id    [[:a {:id 2} [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]]
                      [:c {:id 3} 1 [:pb {:id 4}] 2]
                      [:pb {:id 5, :class "thing"}]],
              :no-id [[:root
                       [:pb {:id 1}]
                       [:a {:id 2} [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]]
                       [:d 3 [:pb {:id 5, :class "thing"}] 4 [:e]]]
                      [:b {} [:c {:id 3} 1 [:pb {:id 4}] 2] 2 [:pb] 3]
                      [:pb]
                      [:d 3 [:pb {:id 5, :class "thing"}] 4 [:e]]
                      [:e]]})))
    (testing ":skip-tree searches should find only outer matching elements"
      (is (= (h/search doc {:pb    (match/hiccup [:pb {:id    true
                                                       :class false}])
                            :id    (match/attr {:id true})
                            :no-id (every-pred
                                     ;; NOTE: to avoid matching the root
                                     (complement (match/tag :root))
                                     (match/attr {:id false}))}
                       :on-match :skip-tree)
             {:id    [[:a {:id 2}
                       [:b {}
                        [:c {:id 3} 1
                         [:pb {:id 4}]
                         2]
                        2
                        [:pb]
                        3]]]
              :no-id [[:d 3
                       [:pb {:class "thing"
                             :id    5}]
                       4
                       [:e]]]
              :pb    [[:pb {:id 1}]]})))
    (testing "a matching root element should terminate the search early"
      (is (= (h/search doc {:pb    (match/hiccup [:pb {:id    true
                                                       :class false}])
                            :id    (match/attr {:id true})
                            ;; matches the root element
                            :no-id (match/attr {:id false})}
                       :exhaustive false)
             {:no-id [doc]})))))

(deftest get-test
  (let [doc [:root
             [:pb {:id 1}]
             [:a {:id 2}
              [:b {}
               [:c {:id 3} 1 [:pb {:id 4}] 2]
               2 [:pb] 3]]
             [:d
              3
              [:pb {:id 5 :class "thing"}]
              4
              [:e]]]]
    (testing "get should return the first occurrence of a matching node"
      (is (= (h/get doc (match/hiccup [:pb {:id    true
                                            :class false}]))
             [:pb {:id 1}]))
      (is (= (h/get doc (match/attr {:id 3}))
             [:c {:id 3} 1 [:pb {:id 4}] 2]))
      (is (= (h/get doc (match/attr {:id false}))
             doc)))))

(deftest split-test
  (let [doc [:root
             [:pb {:id 1}]
             [:a {}
              [:b {}
               [:c {} 1 [:pb {:id 2}] 2]
               2 [:pb {:id 3}] 3]]
             [:d
              3
              [:pb {:id 4 :class "thing"}]
              4
              [:e]]]]
    (testing ":retain false/missing should remove the matched element entirely"
      (is (= (h/split (match/tag :pb) doc :retain false)
             [:root
              [:a {} [:b {} [:c {} 1]]]
              [:a {} [:b {} [:c {} 2] 2]]
              [:a {} [:b {} 3]]
              [:d 3]
              [:d 4 [:e]]]))
      (is (= (h/split (match/tag :pb) doc)
             [:root
              [:a {} [:b {} [:c {} 1]]]
              [:a {} [:b {} [:c {} 2] 2]]
              [:a {} [:b {} 3]]
              [:d 3]
              [:d 4 [:e]]])))
    (testing ":retain :between should place the matched element between splits"
      (is (= (h/split (match/tag :pb) doc :retain :between)
             [:root
              [:pb {:id 1}]
              [:a {} [:b {} [:c {} 1]]]
              [:pb {:id 2}]
              [:a {} [:b {} [:c {} 2] 2]]
              [:pb {:id 3}]
              [:a {} [:b {} 3]]
              [:d 3]
              [:pb {:class "thing"
                    :id    4}]
              [:d 4 [:e]]])))
    (testing ":retain :before should retain the matched element in-place before"
      (is (= (h/split (match/tag :pb) doc :retain :before)
             [:root
              [:pb {:id 1}]
              [:a {} [:b {} [:c {} 1 [:pb {:id 2}]]]]
              [:a {} [:b {} [:c {} 2] 2 [:pb {:id 3}]]]
              [:a {} [:b {} 3]]
              [:d 3 [:pb {:class "thing" :id 4}]]
              [:d 4 [:e]]])))
    (testing ":retain :after should retain the matched element in-place after"
      (is (= (h/split (match/tag :pb) doc :retain :after)
             [:root
              [:pb {:id 1}]
              [:a {} [:b {} [:c {} 1]]]
              [:a {} [:b {} [:c {} [:pb {:id 2}] 2] 2]]
              [:a {} [:b {} [:pb {:id 3}] 3]]
              [:d 3]
              [:d [:pb {:id 4, :class "thing"}] 4 [:e]]])))))

;; TODO: improve this test, mostly by improving HTML conversion itself
(deftest hiccup->text-test
  (is (= (h/hiccup->text example/html5 h/html-text)
         (slurp "test/example.txt"))))

(deftest reshape-test
  (is (= (h/reshape [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
                    {:single {:a          identity
                              (match/any) z/html-safe}
                     :multi  {:x-c            (fn [loc] (zip/append-child loc 123))
                              {:data-id true} (fn [loc] (zip/append-child loc 456))}})
         [:a [:x-b {} "hej " [:x-c {:data-id "blabla"} "med dig" 123 456] " der\n\n"]]))
  (binding [z/*custom-element-prefix* "glen"]
    (is (= (h/reshape [:a [:b "hej " [:c {:id "blabla"} "med dig"] " der\n\n"]]
                      {:single {:a          identity
                                (match/any) z/html-safe}
                       :multi  {:glen-c         (fn [loc] (zip/append-child loc 123))
                                {:data-id true} (fn [loc] (zip/append-child loc 456))}})
           [:a [:glen-b {} "hej " [:glen-c {:data-id "blabla"} "med dig" 123 456] " der\n\n"]]))))
