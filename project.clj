(defproject nested-sets-clj "0.1.0-SNAPSHOT"
  :description "Nested Sets Model for Clojure and ClojureScript"
  :url "https://github.com/toyokumo/nested-sets-clj"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [prismatic/schema "1.1.12"]]
  :repl-options {:init-ns nested-sets.core})
