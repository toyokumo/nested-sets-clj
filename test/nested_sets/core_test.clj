(ns nested-sets.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [nested-sets.core :as sut :refer :all])
  (:import
   (clojure.lang
    ExceptionInfo)))

(deftest root?-test
  (is (true? (sut/root? {:id nil
                         :parent-id nil
                         :lft 1
                         :rgt 2})))
  (is (false? (sut/root? {:id nil
                          :parent-id nil
                          :lft 2
                          :rgt 3}))))

(deftest leaf?-test
  (is (true? (sut/leaf? {:id nil
                         :parent-id nil
                         :lft 1
                         :rgt 2})))
  (is (false? (sut/leaf? {:id nil
                          :parent-id nil
                          :lft 1
                          :rgt 4}))))

(deftest ancestor?-test
  (is (true? (sut/ancestor? {:lft 4 :rgt 11}
                            {:lft 5 :rgt 6})))
  (is (false? (sut/ancestor? {:lft 5 :rgt 6}
                             {:lft 4 :rgt 11})))
  (is (false? (sut/ancestor? {:lft 5 :rgt 6}
                             {:lft 5 :rgt 6}))))

(deftest descendant?-test
  (is (false? (sut/descendant? {:lft 4 :rgt 11}
                               {:lft 5 :rgt 6})))
  (is (true? (sut/descendant? {:lft 5 :rgt 6}
                              {:lft 4 :rgt 11})))
  (is (false? (sut/descendant? {:lft 5 :rgt 6}
                               {:lft 5 :rgt 6}))))

(deftest nested-sets->zipper-test
  (testing "empty arg"
    (is (nil? (sut/nested-sets->vec-tree nil)))
    (is (nil? (sut/nested-sets->vec-tree []))))
  (testing "Only root"
    (let [n1 {:id 1 :parent-id nil :lft 1 :rgt 2}]
      (is (= [n1]
             (sut/nested-sets->vec-tree [n1])))))
  (testing "tree1"
    ;; n1--n2
    ;; |
    ;; n3--n4
    ;; |---n5
    ;; |---n5
    ;; |---n6
    (let [n1 {:id 1 :parent-id nil :lft 1 :rgt 12}
          n2 {:id 2 :parent-id 1 :lft 2 :rgt 3}
          n3 {:id 3 :parent-id 1 :lft 4 :rgt 11}
          n4 {:id 4 :parent-id 3 :lft 5 :rgt 6}
          n5 {:id 5 :parent-id 3 :lft 7 :rgt 8}
          n6 {:id 6 :parent-id 3 :lft 9 :rgt 10}]
      (is (= [n1
              [n2]
              [n3 [n4] [n5] [n6]]]
             (sut/nested-sets->vec-tree [n1 n2 n3 n4 n5 n6])
             (sut/nested-sets->vec-tree (shuffle [n1 n2 n3 n4 n5 n6]))))))
  (testing "complex"
    ;; A-----B-----E
    ;; |
    ;; |-----C-----F----I
    ;; |     |     |
    ;; |     |     ------J----M
    ;; |     |           |
    ;; |     |           -----N
    ;; |      ------G
    ;; ------D----- H------K
    ;;              |
    ;;              -------L
    (let [a {:id :a :lft 1 :rgt 28}
          b {:id :b :lft 2 :rgt 5}
          c {:id :c :lft 6 :rgt 19}
          d {:id :d :lft 20 :rgt 27}
          e {:id :e :lft 3 :rgt 4}
          f {:id :f :lft 7 :rgt 16}
          g {:id :g :lft 17 :rgt 18}
          h {:id :h :lft 21 :rgt 26}
          i {:id :i :lft 8 :rgt 9}
          j {:id :j :lft 10 :rgt 15}
          k {:id :k :lft 22 :rgt 23}
          l {:id :l :lft 24 :rgt 25}
          m {:id :m :lft 11 :rgt 12}
          n {:id :n :lft 13 :rgt 14}]
      (is (= [a
              [b [e]]
              [c [f
                  [i]
                  [j [m] [n]]]
               [g]]
              [d
               [h [k] [l]]]]
             (sut/nested-sets->vec-tree [a b c d e f g h i j k l m n])))))
  (testing "orphan node"
    ;; A-----B-----E
    ;; |
    ;; |-----C
    ;;
    ;; X
    (let [a {:id :a :lft 1 :rgt 8}
          b {:id :b :lft 2 :rgt 5}
          e {:id :e :lft 3 :rgt 4}
          c {:id :c :lft 6 :rgt 7}
          x {:id :x :lft 9 :rgt 10}]
      (is (= {:node x}
             (try
               (sut/nested-sets->vec-tree [a b e c x])
               (catch ExceptionInfo e
                 (ex-data e)))))))
  (testing "customize node"
    ;; A-----B-----C
    ;; |
    ;; |-----D-----E----F
    ;; |     |     |
    ;; |     |     -----G---H
    ;; |      -----I
    (let [a {:id :a :lft 1 :rgt 18}
          b {:id :b :lft 2 :rgt 5}
          c {:id :c :lft 3 :rgt 4}
          d {:id :d :lft 6 :rgt 17}
          e {:id :e :lft 7 :rgt 14}
          f {:id :f :lft 8 :rgt 9}
          g {:id :g :lft 10 :rgt 13}
          h {:id :h :lft 11 :rgt 12}
          i {:id :i :lft 15 :rgt 16}]
      (is (= [:Node a
              [:Node b [:Node c]]
              [:Node d
               [:Node e
                [:Node f]
                [:Node g [:Node h]]]
               [:Node i]]]
             (sut/nested-sets->vec-tree [a b c d e f g h i]
                                        {:make-node (fn [node] [:Node node])}))))))

(deftest adjacency-list->vec-tree-test
  (testing "empty nodes"
    (is (nil? (sut/adjacency-list->vec-tree :id :parent-id nil)))
    (is (nil? (sut/adjacency-list->vec-tree :id :parent-id []))))
  (testing "only root node"
    (let [a {:id :a :parent-id nil}]
      (is (= [a]
             (sut/adjacency-list->vec-tree :id :parent-id [a])))))
  (testing "only leaf under the root"
    (let [a {:id :a :parent-id nil}
          b {:id :b :parent-id :a}
          c {:id :c :parent-id :a}]
      (is (= [a [b] [c]]
             (sut/adjacency-list->vec-tree :id :parent-id [a b c])))))
  (testing "only one subtree"
    (let [a {:id :a :parent-id nil}
          b {:id :b :parent-id :a}
          c {:id :c :parent-id :b}]
      (is (= [a [b [c]]]
             (sut/adjacency-list->vec-tree :id :parent-id [a b c])))))
  (testing "leaf and subtree under the root"
    (let [a {:id :a :parent-id nil}
          b {:id :b :parent-id :a}
          c {:id :c :parent-id :b}
          d {:id :d :parent-id :a}]
      (is (= [a [b [c]] [d]]
             (sut/adjacency-list->vec-tree :id :parent-id [a b c d])))
      (is (= [a [d] [b [c]]]
             (sut/adjacency-list->vec-tree :id :parent-id [a d b c])))))
  (testing "complex"
    ;; A-----B-----E
    ;; |
    ;; |-----C-----F----I
    ;; |     |     |
    ;; |     |     ------J----M
    ;; |     |           |
    ;; |     |           -----N
    ;; |      ------G
    ;; ------D----- H------K
    ;;              |
    ;;              -------L
    (let [a {:id :a :parent-id nil}
          b {:id :b :parent-id :a}
          c {:id :c :parent-id :a}
          d {:id :d :parent-id :a}
          e {:id :e :parent-id :b}
          f {:id :f :parent-id :c}
          g {:id :g :parent-id :c}
          h {:id :h :parent-id :d}
          i {:id :i :parent-id :f}
          j {:id :j :parent-id :f}
          k {:id :k :parent-id :h}
          l {:id :l :parent-id :h}
          m {:id :m :parent-id :j}
          n {:id :n :parent-id :j}]
      (is (= [a
              [b [e]]
              [c [f
                  [i]
                  [j [m] [n]]]
               [g]]
              [d
               [h [k] [l]]]]
             (sut/adjacency-list->vec-tree :id :parent-id [a b c d e f g h i j k l m n])))))
  (testing "customize node"
    ;; A-----B-----C
    ;; |
    ;; |-----D-----E----F
    ;; |     |     |
    ;; |     |     -----G---H
    ;; |      -----I
    (let [a {:id :a :parent-id nil}
          b {:id :b :parent-id :a}
          c {:id :c :parent-id :b}
          d {:id :d :parent-id :a}
          e {:id :e :parent-id :d}
          f {:id :f :parent-id :e}
          g {:id :g :parent-id :e}
          h {:id :h :parent-id :g}
          i {:id :i :parent-id :d}]
      (is (= [:Node a
              [:Node b [:Node c]]
              [:Node d
               [:Node e
                [:Node f]
                [:Node g [:Node h]]]
               [:Node i]]]
             (sut/adjacency-list->vec-tree :id
                                           :parent-id
                                           [a b c d e f g h i]
                                           {:make-node (fn [node] [:Node node])}))))))

(deftest vec-tree->nested-sets-test
  (testing "empty vec"
    (is (nil? (sut/vec-tree->nested-sets nil)))
    (is (nil? (sut/vec-tree->nested-sets []))))
  (testing "only root node"
    (is (= [{:id :a :lft 1 :rgt 2}]
           (sut/vec-tree->nested-sets [{:id :a}]))))
  (testing "only leaf under the root"
    (is (= [{:id :a :lft 1 :rgt 4}
            {:id :b :lft 2 :rgt 3}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}]])))
    (is (= [{:id :a :lft 1 :rgt 6}
            {:id :b :lft 2 :rgt 3}
            {:id :c :lft 4 :rgt 5}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}]
                                       [{:id :c}]]))))
  (testing "only subtree under the root"
    (is (= [{:id :a :lft 1 :rgt 6}
            {:id :b :lft 2 :rgt 5}
            {:id :c :lft 3 :rgt 4}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}
                                        [{:id :c}]]]))))
  (testing "leaf and subtree under the root"
    (is (= [{:id :a :lft 1 :rgt 8}
            {:id :b :lft 2 :rgt 3}
            {:id :c :lft 4 :rgt 7}
            {:id :d :lft 5 :rgt 6}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}]
                                       [{:id :c}
                                        [{:id :d}]]]))))
  (testing "subtree and leaf under the root"
    (is (= [{:id :a :lft 1 :rgt 8}
            {:id :b :lft 2 :rgt 5}
            {:id :c :lft 3 :rgt 4}
            {:id :d :lft 6 :rgt 7}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}
                                        [{:id :c}]]
                                       [{:id :d}]]))))
  (testing "nested subtree under the root"
    (is (= [{:id :a :lft 1 :rgt 12}
            {:id :b :lft 2 :rgt 9}
            {:id :c :lft 3 :rgt 8}
            {:id :d :lft 4 :rgt 7}
            {:id :e :lft 5 :rgt 6}
            {:id :f :lft 10 :rgt 11}]
           (sut/vec-tree->nested-sets [{:id :a}
                                       [{:id :b}
                                        [{:id :c}
                                         [{:id :d}
                                          [{:id :e}]]]]
                                       [{:id :f}]]))))
  (testing "complex"
    ;; A-----B-----E
    ;; |
    ;; |-----C-----F----I
    ;; |     |     |
    ;; |     |     ------J----M
    ;; |     |           |
    ;; |     |           -----N
    ;; |      ------G
    ;; ------D----- H------K
    ;;              |
    ;;              -------L
    (let [a {:id :a :lft 1 :rgt 28}
          b {:id :b :lft 2 :rgt 5}
          e {:id :e :lft 3 :rgt 4}
          c {:id :c :lft 6 :rgt 19}
          f {:id :f :lft 7 :rgt 16}
          i {:id :i :lft 8 :rgt 9}
          j {:id :j :lft 10 :rgt 15}
          m {:id :m :lft 11 :rgt 12}
          n {:id :n :lft 13 :rgt 14}
          g {:id :g :lft 17 :rgt 18}
          d {:id :d :lft 20 :rgt 27}
          h {:id :h :lft 21 :rgt 26}
          k {:id :k :lft 22 :rgt 23}
          l {:id :l :lft 24 :rgt 25}
          coll [a b c d e f g h i j k l m n]]
      (is (= (sort-by :lft coll)
             (sut/vec-tree->nested-sets [a
                                         [b [e]]
                                         [c [f
                                             [i]
                                             [j [m] [n]]]
                                          [g]]
                                         [d
                                          [h [k] [l]]]]))))))

(deftest add-child-test
  (testing "empty nodes"
    (is (= [{:id :a :lft 1 :rgt 2}]
           (add-child {:id :a} nil []))))
  (testing "nodes only has a root"
    (is (= [{:id :a :lft 1 :rgt 4}
            {:id :b :lft 2 :rgt 3}]
           (add-child {:id :b}
                      {:id :a :lft 1 :rgt 2}
                      [{:id :a :lft 1 :rgt 2}]))))
  (testing "tree1"
    ;; A--B--C
    (is (= [{:id :a :lft 1 :rgt 6}
            {:id :b :lft 2 :rgt 5}
            {:id :c :lft 3 :rgt 4}]
           (add-child {:id :c}
                      {:id :b :lft 2 :rgt 3}
                      [{:id :a :lft 1 :rgt 4}
                       {:id :b :lft 2 :rgt 3}]))))
  (testing "tree2"
    ;; A-----B
    ;; |
    ;; |-----C-----D
    ;;             |
    ;;             ---< E >
    ;; Add E
    (let [a {:id :a :lft 1 :rgt 8}
          b {:id :b :lft 2 :rgt 3}
          c {:id :c :lft 4 :rgt 7}
          d {:id :d :lft 5 :rgt 6}
          e {:id :e}]
      (is (= [{:id :a :lft 1 :rgt 10}
              {:id :b :lft 2 :rgt 3}
              {:id :c :lft 4 :rgt 9}
              {:id :d :lft 5 :rgt 8}
              {:id :e :lft 6 :rgt 7}]
             (add-child e
                        d
                        [a b c d]))))))

(deftest add-left-sibling-test
  (testing "empty nodes"
    (is (= [{:id :a :lft 1 :rgt 2}]
           (add-left-sibling {:id :a} nil []))))
  (testing "nodes only has a root"
    (is (= [{:id :a :lft 1 :rgt 6}
            {:id :c :lft 2 :rgt 3}
            {:id :b :lft 4 :rgt 5}]
           (add-left-sibling {:id :c}
                             {:id :b :lft 2 :rgt 3}
                             [{:id :a :lft 1 :rgt 4}
                              {:id :b :lft 2 :rgt 3}]))))
  (testing "tree1"
    ;; A--B
    ;; |
    ;; ---C
    (is (= [{:id :a :lft 1 :rgt 8}
            {:id :b :lft 2 :rgt 3}
            {:id :d :lft 4 :rgt 5}
            {:id :c :lft 6 :rgt 7}]
           (add-left-sibling {:id :d}
                             {:id :c :lft 4 :rgt 5}
                             [{:id :a :lft 1 :rgt 6}
                              {:id :b :lft 2 :rgt 3}
                              {:id :c :lft 4 :rgt 5}]))))
  (testing "tree2"
    ;; A-----B
    ;; |
    ;; |-----C---< E >
    ;;       |
    ;;       ---- D
    ;; Add E
    (let [a {:id :a :lft 1 :rgt 8}
          b {:id :b :lft 2 :rgt 3}
          c {:id :c :lft 4 :rgt 7}
          d {:id :d :lft 5 :rgt 6}
          e {:id :e}]
      (is (= [{:id :a :lft 1 :rgt 10}
              {:id :b :lft 2 :rgt 3}
              {:id :c :lft 4 :rgt 9}
              {:id :e :lft 5 :rgt 6}
              {:id :d :lft 7 :rgt 8}]
             (add-left-sibling e
                               d
                               [a b c d]))))))

(deftest add-right-sibling-test
  (testing "empty nodes"
    (is (= [{:id :a :lft 1 :rgt 2}]
           (add-right-sibling {:id :a} nil []))))
  (testing "nodes only has a root"
    (is (= [{:id :a :lft 1 :rgt 6}
            {:id :b :lft 2 :rgt 3}
            {:id :c :lft 4 :rgt 5}]
           (add-right-sibling {:id :c}
                              {:id :b :lft 2 :rgt 3}
                              [{:id :a :lft 1 :rgt 4}
                               {:id :b :lft 2 :rgt 3}]))))
  (testing "tree1"
     ;; A--B
     ;; |
     ;; ---C
    (is (= [{:id :a :lft 1 :rgt 8}
            {:id :b :lft 2 :rgt 3}
            {:id :c :lft 4 :rgt 5}
            {:id :d :lft 6 :rgt 7}]
           (add-right-sibling {:id :d}
                              {:id :c :lft 4 :rgt 5}
                              [{:id :a :lft 1 :rgt 6}
                               {:id :b :lft 2 :rgt 3}
                               {:id :c :lft 4 :rgt 5}]))))
  (testing "tree2"
    ;; A-----B
    ;; |
    ;; |-----C----D
    ;;       |
    ;;       ----< E >
    ;; Add E
    (let [a {:id :a :lft 1 :rgt 8}
          b {:id :b :lft 2 :rgt 3}
          c {:id :c :lft 4 :rgt 7}
          d {:id :d :lft 5 :rgt 6}
          e {:id :e}]
      (is (= [{:id :a :lft 1 :rgt 10}
              {:id :b :lft 2 :rgt 3}
              {:id :c :lft 4 :rgt 9}
              {:id :d :lft 5 :rgt 6}
              {:id :e :lft 7 :rgt 8}]
             (add-right-sibling e
                                d
                                [a b c d]))))))
