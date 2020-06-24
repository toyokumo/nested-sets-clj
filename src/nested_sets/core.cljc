(ns nested-sets.core
  (:require
   [clojure.zip :as z]
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
  "Make a vector that represents a tree which enable to be a zipper using vector-zip
  nodes must represent nested sets"
  [nodes :- [Node]]
  (when (seq nodes)
    (let [[root & children] (sort-nested-sets nodes)]
      (loop [[node :as rest-nodes] children
             loc (z/vector-zip [root])
             parent-stack [root]]
        (if-not node
          (z/root loc)
          (let [parent (peek parent-stack)]
            (cond
              (nil? parent)
              (throw (ex-info "a orphan node exists" {:node node}))

              (not (ancestor? parent node))
              (recur rest-nodes
                     (z/up loc)
                     (pop parent-stack))

              (leaf? node)
              (recur (rest rest-nodes)
                     (z/append-child loc [node])
                     parent-stack)

              :else
              (recur (rest rest-nodes)
                     (-> loc
                         (z/append-child [node])
                         (z/down)
                         (z/rightmost))
                     (conj parent-stack node)))))))))

(s/defn adjacency-list->vec-tree
  "Make a vector that represents a tree which enable to be a zipper using vector-zip
  nodes must represent adjacency list"
  [id-key :- s/Keyword
   parent-id-key :- s/Keyword
   nodes]
  (when (seq nodes)
    (let [parent-children (group-by parent-id-key nodes)
          [root] (get parent-children nil)]
      (loop [loc (z/vector-zip [root])
             [node & siblings] (get parent-children (id-key root))
             siblings-stack []]
        (cond
          (and (nil? node) (empty? siblings-stack))
          (z/root loc)

          ;; ends processing the level
          (nil? node)
          (recur (z/up loc)
                 (peek siblings-stack)
                 (pop siblings-stack))

          ;; current node has children
          (seq (get parent-children (:id node)))
          (let [children (get parent-children (:id node))]
            (recur (-> loc
                       (z/append-child [node])
                       (z/down)
                       (z/rightmost))
                   ;; make children level processed
                   children
                   ;; save same level nodes that isn't processed
                   (conj siblings-stack siblings)))

          :else
          (recur (z/append-child loc [node])
                 siblings
                 siblings-stack))))))

(s/defn vec-tree->nested-sets :- (s/maybe [Node])
  "Make a nested sets as a vector from a vector tree
  which may be made from nested-sets->vec-tree above"
  [vec-tree]
  (when (seq vec-tree)
    (loop [loc (z/vector-zip vec-tree)
           i 1
           acc []
           parent-stack []]
      (cond
        (z/end? loc)
        acc

        (z/branch? loc)
        (recur (z/next loc)
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

        (z/branch? (z/prev loc))
        (let [children (-> loc z/prev z/children rest)
              node (assoc (z/node loc)
                          :lft i
                          :rgt (if (seq children)
                                 (+ 1 i (* 2 (count (flatten children))))
                                 (inc i)))]
          (recur (z/next loc)
                 (inc i)
                 (conj acc node)
                 (conj parent-stack node)))

        :else
        (let [node (assoc (z/node loc)
                          :lft i
                          :rgt (inc i))]
          (recur (z/next loc)
                 (inc (:rgt node))
                 (conj acc node)
                 parent-stack))))))
