(defproject suprematic/datomic-helpers "0.1.0-SNAPSHOT"

  :description "Helpers for declaring Datomic schema"
  :url "https://github.com/suprematic/datomic-helpers"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure "1.10.0"]]

  :profiles
  {:dev
   {:repositories
    {"my.datomic.com"
     {:url "https://my.datomic.com/repo"
      :username ~(System/getenv "DATOMIC_USERNAME")
      :password ~(System/getenv "DATOMIC_ACCESS_KEY")}}

    :dependencies
    [[com.datomic/datomic-pro "0.9.5927"]]}})
