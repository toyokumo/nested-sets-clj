(ns nested-sets.core
  (:require
   [clojure.zip :as z]))

(defn root?
  "true if the node is the root node"
  [{:keys [lft]}]
  (= lft 1))

(defn leaf?
  "true if the node is a leaf node"
  [{:keys [lft rgt]}]
  (= (- rgt lft) 1))

(defn ancestor?
  "true if n1 is a ancestor of n2"
  [n1 n2]
  (< (:lft n1) (:lft n2) (:rgt n1)))

(defn descendant?
  "true if n1 is a descendant of n2"
  [n1 n2]
  (< (:lft n2) (:lft n1) (:rgt n2)))

(defn sort-nested-sets
  "sort as nested sets model"
  [nodes]
  (sort-by :lft nodes))

(defn nested-sets->vec-tree
  "Make a vector that represents a tree which enable to be a zipper using vector-zip
  nodes must represent nested sets

  This can take a options.
   :make-node - a function that, given a node, returns a vector which represents a node of a tree
                default: clojure.core/vector in clj or cljs.core/vector in cljs"
  [nodes & [opts]]
  (when (seq nodes)
    (let [make-node (or (:make-node opts) vector)
          [root & children] (sort-nested-sets nodes)]
      (loop [[node :as rest-nodes] children
             loc (z/vector-zip (make-node root))
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
                     (z/append-child loc (make-node node))
                     parent-stack)

              :else
              (recur (rest rest-nodes)
                     (-> loc
                         (z/append-child (make-node node))
                         (z/down)
                         (z/rightmost))
                     (conj parent-stack node)))))))))

(defn adjacency-list->vec-tree
  "Make a vector that represents a tree which enable to be a zipper using vector-zip
  nodes must represent adjacency list

  id-key        - a keyword that represents id of a node
  parent-id-key - a keyword that represents parent id of a node

  This can take a options.
   :make-node - a function that, given a node, returns a vector which represents a node of a tree
                default: clojure.core/vector in clj or cljs.core/vector in cljs"
  [id-key parent-id-key nodes & [opts]]
  (when (seq nodes)
    (let [make-node (or (:make-node opts) vector)
          parent-children (group-by parent-id-key nodes)
          [root] (get parent-children nil)]
      (loop [loc (z/vector-zip (make-node root))
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
          (seq (get parent-children (id-key node)))
          (let [children (get parent-children (id-key node))]
            (recur (-> loc
                       (z/append-child (make-node node))
                       (z/down)
                       (z/rightmost))
                   ;; make children level processed
                   children
                   ;; save same level nodes that isn't processed
                   (conj siblings-stack siblings)))

          :else
          (recur (z/append-child loc (make-node node))
                 siblings
                 siblings-stack))))))

(defn vec-tree->nested-sets
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

(defn add-child
  "Add a new-node to the nodes as a child of the parent-node"
  [new-node parent-node nodes]
  (if (empty? nodes)
    [(assoc new-node :lft 1 :rgt 2)]
    (loop [loc (z/vector-zip (nested-sets->vec-tree nodes))]
      (cond
        ;; parent-node doesn't exist
        (z/end? loc)
        (vec-tree->nested-sets (z/root loc))

        (z/branch? loc)
        (recur (z/next loc))

        ;; found the parent
        (= (z/node loc) parent-node)
        (-> loc
            (z/insert-right [new-node])
            (z/root)
            (vec-tree->nested-sets))

        :else
        (recur (z/next loc))))))

(defn add-left-sibling
  "Add a new-node to the nodes as a left sibling of the focus-node"
  [new-node focus-node nodes]
  (assert (not (root? focus-node)))
  (if (empty? nodes)
    [(assoc new-node :lft 1 :rgt 2)]
    (loop [loc (z/vector-zip (nested-sets->vec-tree nodes))]
      (cond
        ;; focus-node doesn't exist
        (z/end? loc)
        (vec-tree->nested-sets (z/root loc))

        (z/branch? loc)
        (recur (z/next loc))

        ;; found the focus-node
        (= (z/node loc) focus-node)
        (-> loc
            (z/up)
            (z/insert-left [new-node])
            (z/root)
            (vec-tree->nested-sets))

        :else
        (recur (z/next loc))))))

(defn add-right-sibling
  "Add a new-node to the nodes as a right sibling of the focus-node"
  [new-node focus-node nodes]
  (assert (not (root? focus-node)))
  (if (empty? nodes)
    [(assoc new-node :lft 1 :rgt 2)]
    (loop [loc (z/vector-zip (nested-sets->vec-tree nodes))]
      (cond
        ;; focus-node doesn't exist
        (z/end? loc)
        (vec-tree->nested-sets (z/root loc))

        (z/branch? loc)
        (recur (z/next loc))

        ;; found the focus-node
        (= (z/node loc) focus-node)
        (-> loc
            (z/up)
            (z/insert-right [new-node])
            (z/root)
            (vec-tree->nested-sets))

        :else
        (recur (z/next loc))))))
