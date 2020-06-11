(ns nested-sets.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [nested-sets.core :as sut])
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
    (let [n1 {:id 1 :parent-id nil :lft 1 :rgt 12}
          n2 {:id 2 :parent-id 1 :lft 2 :rgt 3}
          n3 {:id 3 :parent-id 1 :lft 4 :rgt 11}
          n4 {:id 4 :parent-id 3 :lft 5 :rgt 6}
          n5 {:id 5 :parent-id 3 :lft 7 :rgt 8}
          n6 {:id 6 :parent-id 3 :lft 9 :rgt 10}]
      (is (= [n1 n2 [n3 n4 n5 n6]]
             (sut/nested-sets->vec-tree [n1 n2 n3 n4 n5 n6])
             (sut/nested-sets->vec-tree (shuffle [n1 n2 n3 n4 n5 n6]))))))
  (testing "tree2"
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
              [b e]
              [c [f i [j m n]] g]
              [d [h k l]]]
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
                 (ex-data e))))))))
