(ns datomic-helpers.schema
  (:require [datomic.api :as db]
            [clojure.string :as str]))


;; ====================================================================
;; Internal
;; ====================================================================


(defn check-attribute-definition [attr]
  (assert
    (keyword? (:db/ident attr))
    (format
      ":db/ident must be a keyword, got %s, attribute definition: %s"
      (:db/ident attr) attr))
  (assert
    (if (contains? attr :db/doc)
      (-> attr :db/doc string?)
      true)
    (format
      ":db/doc must be a string, got %s, attribute definition: %s"
      (:db/doc attr) attr))
  (assert
    (if (contains? attr :db/valueType)
      (-> attr :db/valueType keyword?)
      true)
    (format
      ":db/valueType must be a keyword, got %s, attribute definition: %s"
      (:db/valueType attr) attr))
  (assert
    (if (contains? attr :db/cardinality)
      (-> attr :db/cardinality keyword?)
      true)
    (format
      ":db/cardinality must be a keyword, got %s, attribute definition: %s"
      (:db/cardinality attr) attr))
  (assert
    (if (contains? attr :db/unique)
      (-> attr :db/unique keyword?)
      true)
    (format
      ":db/unique must be a keyword, got %s, attribute definition: %s"
      (:db/unique attr) attr))
  (assert
    (if (contains? attr :db/isComponent)
      (-> attr :db/isComponent boolean?)
      true)
    (format
      ":db/isComponent must be a keyword, got %s, attribute definition: %s"
      (:db/isComponent attr) attr)))


(defn db-fn* [fname args code docs]
  (merge
    {:db/ident (keyword fname)
     :db/fn (db/function
              {:lang :clojure
               :params args
               :code code})}
    (when docs
      {:db/doc docs})))


(defn collect-db-fns* [ns]
  (->> (ns-interns ns)
    (vals)
    (mapv meta)
    (mapv ::db-fn)
    (filter some?)))


(defn collect-schemas* [ns]
  (->> (ns-interns ns)
    (vals)
    (filter #(-> % meta ::db-schema))
    (map deref)
    (apply concat)))


(defn check-duplicate-attrs [schema]
  (let [duplicates
        (->> schema
          (group-by :db/ident)
          (vals)
          (filter #(> (count %) 1)))]
    (assert
      (empty? duplicates)
      (format
        "Schema contains multiple definitions of the following attributes: %s"
        (->> duplicates
          (map first)
          (map :db/ident)
          (str/join ", "))))))


(defn ->idents [group-name ident-keys]
  (assert
    (every? keyword? ident-keys)
    (format
      "identity name must be a keyword, got %s (group: %s)"
      (first (filter (complement keyword?) ident-keys))
      group-name))
  (mapv #(hash-map :db/doc group-name :db/ident %) ident-keys))


(defmacro collect-db-fns []
  `(collect-db-fns* *ns*))


(defmacro collect-schemas []
  `(collect-schemas* *ns*))


;; ====================================================================
;; API
;; ====================================================================


(defmacro defidents
  "Defines a private var `group-name` binding it to a list of identity
  definitions. `idents` are keywords which will be used as :db/ident values.

  The code below

  ```
  (defidents color
    :color/red
    :color/yellow)
  ```

  is similar to

  ```
  (def ^:private color
    [{:db/ident :color/red
      :db/doc \"color\"}
     {:db/ident :color/yellow
      :db/doc \"color\"}])
  ```

  Throws on invalid arguments."
  [group-name & idents]
  (let [group-name (vary-meta group-name assoc ::db-schema true)
        group-str (name group-name)]
    `(def ^:private ~group-name (->idents ~group-str [~@idents]))))


(defmacro db-fn [fname docs-or-args & more]
  (let [[docs args body]
        (cond
          (string? docs-or-args)
          [docs-or-args (first more) (rest more)]

          (vector? docs-or-args)
          [nil docs-or-args more]

          :else
          (throw (IllegalArgumentException. "args must be a vector")))
        fname-str (name fname)
        fcode (cons 'do body)]
    `(do
       (defn- ~fname ~args ~@body)
       (alter-meta!
         #'~fname assoc ::db-fn (db-fn* ~fname-str '~args '~fcode '~docs)))))


(defmacro defschema [schema-name & attr-defs]
  (let [schema-meta {::db-schema true}
        schema-name (vary-meta schema-name merge schema-meta)]
    `(do
       (doseq [attr# [~@attr-defs]]
         (check-attribute-definition attr#))
       (def ^:private ~schema-name [~@attr-defs]))))


(defmacro collect-schema []
  `(let [schema# (concat (collect-schemas) (collect-db-fns))]
     (check-duplicate-attrs schema#)
     schema#))
