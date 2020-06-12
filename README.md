# nested-sets-clj
It helps to use [Nested Sets Model](https://en.wikipedia.org/wiki/Nested_set_model) with Clojure and ClojureScript.

[![CircleCI](https://circleci.com/gh/toyokumo/nested-sets-clj.svg?style=svg&circle-token=c5ad3729a43000831bdfa56adb625c0584ea0b38)](https://circleci.com/gh/toyokumo/nested-sets-clj)
[![cljdoc badge](https://cljdoc.org/badge/toyokumo/nested-sets-clj)](https://cljdoc.org/d/toyokumo/nested-sets-clj/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/toyokumo/nested-sets-clj.svg)](https://clojars.org/toyokumo/nested-sets-clj)

## Usage

Make a vector tree from nested sets which is sometimes gotton from DB.

```clj
(require '[nested-sets.core :as nested])

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
  (nested/nested-sets->vec-tree [a b c d e f g h i j k l m n]))
;=>
[{:id :a, :lft 1, :rgt 28}
 [{:id :b, :lft 2, :rgt 5} {:id :e, :lft 3, :rgt 4}]
 [{:id :c, :lft 6, :rgt 19}
  [{:id :f, :lft 7, :rgt 16}
   {:id :i, :lft 8, :rgt 9}
   [{:id :j, :lft 10, :rgt 15} {:id :m, :lft 11, :rgt 12} {:id :n, :lft 13, :rgt 14}]]
  {:id :g, :lft 17, :rgt 18}]
 [{:id :d, :lft 20, :rgt 27}
  [{:id :h, :lft 21, :rgt 26} {:id :k, :lft 22, :rgt 23} {:id :l, :lft 24, :rgt 25}]]]
```

On the contary, it enable to make nested sets form a vector tree.

```clj
(require '[nested-sets.core :as nested])

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
(let [a {:id :a}
      b {:id :b}
      c {:id :c}
      d {:id :d}
      e {:id :e}
      f {:id :f}
      g {:id :g}
      h {:id :h}
      i {:id :i}
      j {:id :j}
      k {:id :k}
      l {:id :l}
      m {:id :m}
      n {:id :n}]
  (nested/vec-tree->nested-sets [a
                                 [b e]
                                 [c [f i [j m n]] g]
                                 [d [h k l]]]))
;=>
[{:id :a, :lft 1, :rgt 28}
 {:id :b, :lft 2, :rgt 5}
 {:id :e, :lft 3, :rgt 4}
 {:id :c, :lft 6, :rgt 19}
 {:id :f, :lft 7, :rgt 16}
 {:id :i, :lft 8, :rgt 9}
 {:id :j, :lft 10, :rgt 15}
 {:id :m, :lft 11, :rgt 12}
 {:id :n, :lft 13, :rgt 14}
 {:id :g, :lft 17, :rgt 18}
 {:id :d, :lft 20, :rgt 27}
 {:id :h, :lft 21, :rgt 26}
 {:id :k, :lft 22, :rgt 23}
 {:id :l, :lft 24, :rgt 25}]
```

It also provides a function to make a vector tree from adjacency list.

```clj
(require '[nested-sets.core :as nested])

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
  (nested/adjacency-list->vec-tree :id :parent-id [a b c d e f g h i j k l m n]))
;=>
[{:id :a, :parent-id nil}
 [{:id :b, :parent-id :a} {:id :e, :parent-id :b}]
 [{:id :c, :parent-id :a}
  [{:id :f, :parent-id :c}
   {:id :i, :parent-id :f}
   [{:id :j, :parent-id :f} {:id :m, :parent-id :j} {:id :n, :parent-id :j}]]
  {:id :g, :parent-id :c}]
 [{:id :d, :parent-id :a} [{:id :h, :parent-id :d} {:id :k, :parent-id :h} {:id :l, :parent-id :h}]]]
```

## License

Copyright 2020 TOYOKUMO,Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
