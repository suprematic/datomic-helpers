(ns datomic-helpers.schema-test
  (:require [datomic-helpers.schema :as dschema]
            [datomic.api :as datomic]
            [clojure.test :refer [deftest is]]))


;; ====================================================================
;; Internal
;; ====================================================================


(def ^:private db-url "datomic:mem://schema-test")

(defn- reset-db []
  (datomic/delete-database db-url)
  (datomic/create-database db-url))


;; ====================================================================
;; defidents
;; ====================================================================


(deftest defidents--no-definitions
  (let [idents-sym (gensym "idents--")]
    (eval (list `dschema/defidents idents-sym))
    (is (empty? @(resolve idents-sym)))))


(deftest defidents--ok
  (let [idents-sym (gensym "idents--")]
    (reset-db)
    (eval (list `dschema/defidents idents-sym
            ::dschema/test1
            ::dschema/test2))
    (let [db-conn (datomic/connect db-url)]
      (is @(datomic/transact db-conn @(resolve idents-sym)))
      (let [db (datomic/db db-conn)
            entities
            (datomic/pull-many db '[*] [::dschema/test1 ::dschema/test2])]
        (is
          (= [::dschema/test1 ::dschema/test2]
            (mapv :db/ident entities)))
        (is (every? #{(name idents-sym)} (mapv :db/doc entities)))))))


(deftest defidents--invalid-args
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/defidents "abc"))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/defidents 1))))
  (let [idents-sym (gensym "idents--")]
    (is
      (thrown? clojure.lang.Compiler$CompilerException
        (eval (list `dschema/defidents idents-sym {}))))
    (is
      (thrown? clojure.lang.Compiler$CompilerException
        (eval (list `dschema/defidents idents-sym "abc"))))
    (is
      (thrown? clojure.lang.Compiler$CompilerException
        (eval (list `dschema/defidents idents-sym 1 2))))))


;; ====================================================================
;; db-fn
;; ====================================================================


(deftest db-fn--ok
  (let [f-sym (gensym "db-fn--")]
    (eval (list `dschema/db-fn f-sym ['_db] [[:test]]))
    (is (= [[:test]] ((resolve f-sym) nil)))))


(deftest db-fn--fn-compilation-error
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn "f" [] :ok))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn 'f {}))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn [] :ok))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn 'f 'docs []))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn 'f [] '(let [a] :ok)))))
  (is
    (thrown? clojure.lang.Compiler$CompilerException
      (eval (list `dschema/db-fn 'f [] '(do x))))))


(deftest db-fn--transaction-call-no-args
  (let [f-sym (gensym "db-fn--")
        f-key (-> f-sym name keyword)]
    (reset-db)
    (eval (list `dschema/db-fn f-sym ['_db] [{:db/ident ::dschema/test}]))
    (let [db-conn (datomic/connect db-url)
          db-fn (-> (resolve f-sym) meta ::dschema/db-fn)]
      (is @(datomic/transact db-conn [db-fn]))
      (is @(datomic/transact db-conn [[f-key]]))
      (let [db (datomic/db db-conn)
            entity (datomic/pull db '[*] ::dschema/test)]
        (is (= ::dschema/test (:db/ident entity)))))))


(deftest db-fn--transaction-call-with-args
  (let [f-sym (gensym "db-fn--")
        f-key (-> f-sym name keyword)]
    (reset-db)
    (eval (list `dschema/db-fn f-sym ['_db 'x]
            '(case x :test [{:db/ident ::dschema/test}])))
    (let [db-conn (datomic/connect db-url)
          db-fn (-> (resolve f-sym) meta ::dschema/db-fn)]
      (is @(datomic/transact db-conn [db-fn]))
      (is @(datomic/transact db-conn [[f-key :test]]))
      (let [db (datomic/db db-conn)
            entity (datomic/pull db '[*] ::dschema/test)]
        (is (= ::dschema/test (:db/ident entity)))))))
