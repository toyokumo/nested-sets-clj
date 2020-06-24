# nested-sets-clj
It helps to use [Nested Sets Model](https://en.wikipedia.org/wiki/Nested_set_model) with Clojure and ClojureScript.

[![CircleCI](https://circleci.com/gh/toyokumo/nested-sets-clj.svg?style=svg&circle-token=c5ad3729a43000831bdfa56adb625c0584ea0b38)](https://circleci.com/gh/toyokumo/nested-sets-clj)
[![cljdoc badge](https://cljdoc.org/badge/toyokumo/nested-sets-clj)](https://cljdoc.org/d/toyokumo/nested-sets-clj/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/toyokumo/nested-sets-clj.svg)](https://clojars.org/toyokumo/nested-sets-clj)

## Usage

Make a vector tree from nested sets which is sometimes gotton from DB.

```clj
(require '[nested-sets.core :as nested])

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
  (= [a
      [b [c]]
      [d
       [e
        [f]
        [g [h]]]
       [i]]]
     (nested/nested-sets->vec-tree [a b c d e f g h i])))
;=> true
```

On the contary, it enable to make nested sets form a vector tree.

```clj
(require '[nested-sets.core :as nested])

;; A-----B-----C
;; |
;; |-----D-----E----F
;; |     |     |
;; |     |     -----G---H
;; |      -----I
(let [a {:id :a}
      b {:id :b}
      c {:id :c}
      d {:id :d}
      e {:id :e}
      f {:id :f}
      g {:id :g}
      h {:id :h}
      i {:id :i}]
  (= [{:id :a :lft 1 :rgt 18}
      {:id :b :lft 2 :rgt 5}
      {:id :c :lft 3 :rgt 4}
      {:id :d :lft 6 :rgt 17}
      {:id :e :lft 7 :rgt 14}
      {:id :f :lft 8 :rgt 9}
      {:id :g :lft 10 :rgt 13}
      {:id :h :lft 11 :rgt 12}
      {:id :i :lft 15 :rgt 16}]
     (nested/vec-tree->nested-sets [a
                                    [b [c]]
                                    [d
                                     [e
                                      [f]
                                      [g [h]]]
                                     [i]]])))
;=> true
```

It also provides a function to make a vector tree from adjacency list.

```clj
(require '[nested-sets.core :as nested])

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
  (= [a
      [b [c]]
      [d
       [e
        [f]
        [g [h]]]
       [i]]]
     (nested/adjacency-list->vec-tree :id :parent-id [a b c d e f g h i])))
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
