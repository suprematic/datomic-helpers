# datomic-helpers

Helpers for declaring Datomic schema.

## Rationale

Datomic schema is a list of facts defined as Clojure data, which is great. But
Datomic doesn't tell how to organize DB schema. The simplest option would be
defining a single list of database objects.
That is fine for small schemas. But when schema grows, it becomes hard to manage
and validate it:

- while developing, you may want to turn on/off some attribute definitions
- text editors don't like huge Clojure forms

Also, it's better to ensure that

- all the schema entities have unique names
- DB functions are syntactically correct and don't refer undefined symbols

datomic-helpers is an attempt to solve the problems above adding some
convenience to defining Datomic database schemas.

## Usage

Add dependency

```clojure
[suprematic/datomic-helpers "0.1.0-SNAPSHOT"]
```

Require `datomic-helpers.schema`

```clojure
(ns my.app.store-db
  (:require [datomic-helpers.schema as dschema
             :refer [defschema db-fn defidents]]))
```

## Examples

Define identities

```clojure
;; defidents creates a list or identity definitions and binds it to a var
(defidents colors
  :color/red
  :color/yellow
  :color/blue
  :color/green
  :color/black)
```

Define attributes

```clojure
;; defschema creates a list or attribute definitions and binds it to a var
(defschema pen
  {:db/ident :pen/ink-color
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "The ink color"}

  {:db/ident :pen/cap-color
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "The cap color"})

(defschema pencil
  {:db/ident :pencil/core-color
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "The ink color"}

  {:db/ident :pencil/body-color
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc "The cap color"})
```

Define DB functions

```clojure
;; Internal checks

;; db-fn also defines a function which can be used e.g., in REPL
(db-fn -check-pen-attributes [_db pen]
  (when-not (contains? pen :pen/ink-color)
    (throw (ex-info "no ink color" {:pen pen})))
  (when-not (contains? pen :pen/cap-color)
    (throw (ex-info "no cap color" {:pen pen}))))


(db-fn -check-pencil-attributes [_db pencil]
  (when-not (contains? pencil :pencil/core-color)
    (throw (ex-info "no core color" {:pencil pencil})))
  (when-not (contains? pencil :pencil/body-color)
    (throw (ex-info "no body color" {:pencil pencil}))))

;; API

(db-fn add-pen [_db pen]
  [[:-check-pen-attributes pen]
   pen])

(db-fn add-pencil [_db pencil]
  [[:-check-pencil-attributes pencil]
   pencil])
```

Collect the final schema, checking if all the definitions are unique

```clojure
(def schema
  (dschema/collect-schema))
```

Write the schema and use the DB

```clojure
(ns my.app
  (:require [datomic.api :as datomic]
            [my.app.store-db :as store-db]))

(let [db-conn (datomic/connect db-url)]
  @(datomic/transact db-conn store-db/schema))
  
(let [pen
      {:pen/ink-color :color/blue
       :pen/cap-color :color/black}
      pencil
      {:pencil/core-color :color/yellow
       :pencil/body-color :color/yellow}
      db-conn (datomic/connect db-url)]
  @(datomic/transact db-conn [[:add-pen pen]])
  @(datomic/transact db-conn [[:add-pencil pencil]]))
```

## License

Copyright © 2020 Suprematic

This library and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
[eclipse.org](http://www.eclipse.org/legal/epl-2.0).

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at [gnu.org](https://www.gnu.org/software/classpath/license.html).
