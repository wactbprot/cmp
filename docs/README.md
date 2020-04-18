# Documentation

* [API](./api)
* [coverage](./coverage)
* [namespaces](./graph.png)


# Usage

User interaction with **cmp** is **REPL only**. The `core`
namespace is the entry point. Use `(dir cmp.core)` to get an
overview. Commands in `(ns cmp.core)` have the following prefixes:

* `t-` ... **tasks**
- `t-build (cmp.core) <f>`
- `t-build-edn (cmp.core) <f>`
- `t-clear (cmp.core) <f>`
- `t-refresh (cmp.core) <f>`

* `m-` ... **mpd**
- `m-build (cmp.core) <f>`
- `m-build-edn (cmp.core) <f>`
- `m-clear (cmp.core) <f>`
- `m-info (cmp.core) <f>`
- `m-start (cmp.core) <f>`
- `m-stop (cmp.core) <f>`

* `d-` ... **documents**
- `d-add (cmp.core) <f>`
- `d-ids (cmp.core) <f>`
- `d-rm (cmp.core) <f>`

* `c-` ... **container**
- `c-reset (cmp.core) <f>`
- `c-run (cmp.core) <f>`
- `c-status (cmp.core) <f>`
- `c-stop (cmp.core) <f> `	

* `n-` ... **definitions**
- `n-status (cmp.core) <f>`


## reference mpd

Find a documented reference measurement program definition
(`mpd`) in [edn-format](https://github.com/edn-format/edn) 
at [resources/mpd-ref.edn](../resources/mpd-ref.edn).
This `mpd` named `"ref"` can be used as a example.

Build or refresh tasks with:
```clojure
(refresh-tasks)
```

Build `edn` examples with:

```clojure
(build-ref-edn)
```

followed by:

```clojure
(workon! "ref")
(start-observe)
(run-c 0) ;; abbrev. for (ctrl 0 "run") 
```

Use `(doc build-tasks)` for a more detailed view.

```clojure
(doc build-tasks)
-------------------------
cmp.core/build-tasks
([])
  Builds the `tasks` endpoint. At
  runtime all `tasks` are provided by
  `st-mem` 
```

```clojure
(workon!! mp-id)
(run-c 0)
(stat-c 0)
```

## documents

To add or rm documents for storing data in use
`(doc-add mp-id doc-id)`, `(doc-rm mp-id doc-id)`.
If `(->mp-id)` is set (by `(workon mp-id)`)
`(doc-add doc-id)`, `(doc-rm doc-id)` is sufficient.

```clojure
(doc-add "cal-2020-se3-kk-11111_0002")
;; hiob DEBUG [cmp.lt-mem:14] - try to get document
;;             with id: cal-2020-se3-kk-11111_0002
;; "OK"
(doc-ids)
;; (cal-2020-se3-kk-11111_0001 cal-2020-se3-kk-11111_0002)
```

# Clojure

* [cheatsheet](https://clojure.org/api/cheatsheet)
* [clojure-style-guide](https://github.com/bbatsov/clojure-style-guide)
* [eastwood (linter)](https://github.com/jonase/eastwood)

# Redis
## config

All of the `mp` state is kept in a [redis](https://redis.io) database.

Since version 0.3.0 *cmp* relies on [Keyspace Notifications](https://redis.io/topics/notifications).
Therefore it is necassary to replace in `/etc/redis/redis.conf`:

```shell
notify-keyspace-events ""
```

by

```shell
notify-keyspace-events AK

```

and restart the service:


```shell
# restart
sudo systemctl restart redis.service

# check state
sudo systemctl status redis.service
```

## redis gui

* [RedisDesktopManager](https://github.com/uglide/RedisDesktopManager) `sudo snap install redis-desktop-manager`
* [redis-commander](https://github.com/joeferner/redis-commander)

```shell
$ npm install -g redis-commander
## --> http://localhost:8081/
```

# devel
## documentation

(re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```

## tests

```shell
$ cd path/to/cmp
$ lein test
```

## code coverage

```shell
$ cd path/to/cmp
$ lein cloverage
```

## ns-graph

```shell
$ cd path/to/cmp
$ lein ns-graph
```
