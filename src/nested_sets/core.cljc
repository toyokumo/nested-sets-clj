(ns nested-sets.core
  (:require
   [clojure.zip :as zip]
   [schema.core :as s]))

(s/defschema Node
  {:lft s/Int
   :rgt s/Int
   s/Keyword s/Any})

(s/defn root?
  "true if the node is the root node"
  [{:keys [lft]} :- Node]
  (= lft 1))

(s/defn leaf?
  "true if the node is a leaf node"
  [{:keys [lft rgt]} :- Node]
  (= (- rgt lft) 1))

(s/defn ancestor?
  "true if n1 is a ancestor of n2"
  [n1 :- Node
   n2 :- Node]
  (< (:lft n1) (:lft n2) (:rgt n1)))

(s/defn descendant?
  "true if n1 is a descendant of n2"
  [n1 :- Node
   n2 :- Node]
  (< (:lft n2) (:lft n1) (:rgt n2)))

(s/defn sort-nested-sets
  "sort as nested sets model"
  [nodes :- [Node]]
  (sort-by :lft nodes))

(s/defn nested-sets->vec-tree
  "Make a vector that represents a tree which enable to be a zipper using vector-zip"
  [nodes :- [Node]]
  (when (seq nodes)
    (let [[root & children] (sort-nested-sets nodes)]
      (loop [[node :as rest-nodes] children
             loc (zip/vector-zip [root])
             parent-stack [root]]
        (if-not node
          (zip/root loc)
          (let [parent (peek parent-stack)]
            (cond
              (nil? parent)
              (throw (ex-info "a orphan node exists" {:node node}))

              (not (ancestor? parent node))
              (recur rest-nodes
                     (zip/up loc)
                     (pop parent-stack))

              (leaf? node)
              (recur (rest rest-nodes)
                     (zip/append-child loc node)
                     parent-stack)

              :else
              (recur (rest rest-nodes)
                     (-> loc
                         (zip/append-child [])
                         (zip/down)
                         (zip/rightmost)
                         (zip/append-child node))
                     (conj parent-stack node)))))))))

(s/defn vec-tree->nested-sets :- (s/maybe [Node])
  "Make a nested sets as a vector from a vector tree
  which may be made from nested-sets->vec-tree above"
  [vec-tree]
  (when (seq vec-tree)
    (loop [loc (zip/vector-zip vec-tree)
           i 1
           acc []
           parent-stack []]
      (cond
        (zip/end? loc)
        acc

        (zip/branch? loc)
        (recur (zip/next loc)
               i
               acc
               parent-stack)

        ;; go out parents recursively
        (and (seq parent-stack)
             (<= (:rgt (peek parent-stack)) i))
        (recur loc                                          ;not move
               (inc i)
               acc
               (pop parent-stack))

        (zip/branch? (zip/prev loc))
        (let [children (-> loc zip/prev zip/children rest)
              node (assoc (zip/node loc)
                          :lft i
                          :rgt (if (seq children)
                                 (+ 1 i (* 2 (count (flatten children))))
                                 (inc i)))]
          (recur (zip/next loc)
                 (inc i)
                 (conj acc node)
                 (conj parent-stack node)))

        :else
        (let [node (assoc (zip/node loc)
                          :lft i
                          :rgt (inc i))]
          (recur (zip/next loc)
                 (inc (:rgt node))
                 (conj acc node)
                 parent-stack))))))
