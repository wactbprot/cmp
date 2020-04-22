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
- `c-suspend (cmp.core) <f>`

* `n-` ... **definitions**
- `n-status (cmp.core) <f>`


## reference mpd

Find a documented reference measurement program definition (`mpd`) in
[edn-format](https://github.com/edn-format/edn) at
[resources/mpd-ref.edn](../resources/mpd-ref.edn).  This `mpd` named
`"ref"` can be used as a example.

Build `mpd` provided by *cmp* in `edn`-format with:

```clojure
(m-build-edn)
```

## tasks

Build or refresh tasks with:

```clojure
(t-refresh)
```

Build `tasks` provided by *cmp* in `edn`-format with:

```clojure
(t-build-edn)
```

`(t-table)` overview of all tasks loaded in short term memory.
The table may be filtered:

![cmp](docs/t-table.png)



## start mpd

```clojure
(workon! "ref")
(m-start)
```

## run container

Run the first *container* with:

```clojure
(c-run 0)
;; same as:
(ctrl 0 "run")
```

## go on

Use the build-in `(doc x)` function (e.g. `(doc t-build)`) for further
details.

```clojure
(doc t-build)
-------------------------
cmp.core/t-build
([])
  Builds the `tasks` endpoint. At
  runtime all `tasks` are provided by
  `st-mem`
```

Use the build-in `(dir cmp.core)` function to get a list of all
functions in this namespace.

## documents

To add or rm documents for storing data in use `(d-add mp-id doc-id)`,
`(d-rm mp-id doc-id)`.  If `(->mp-id)` is set (by `(workon mp-id)`)
`(d-add doc-id)`, `(d-rm doc-id)` is sufficient.

```clojure
(d-add "cal-2020-se3-kk-11111_0002")
;; hiob DEBUG [cmp.lt-mem:14] - try to get document
;;             with id: cal-2020-se3-kk-11111_0002
;; "OK"
(d-ids)
;; (cal-2020-se3-kk-11111_0001 cal-2020-se3-kk-11111_0002)
```

# Clojure

* [cheatsheet](https://clojure.org/api/cheatsheet)
* [clojure-style-guide](https://github.com/bbatsov/clojure-style-guide)
* [eastwood (linter)](https://github.com/jonase/eastwood)

# Redis

All of the `mp` state is kept in a [redis](https://redis.io) database.

## config

Since version 0.3.0 *cmp* relies on
[Keyspace Notifications](https://redis.io/topics/notifications).
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
$ sudo systemctl restart redis.service

# check state
$ sudo systemctl status redis.service
```

## redis gui

* [RedisDesktopManager](https://github.com/uglide/RedisDesktopManager)
  `sudo snap install redis-desktop-manager`
* [redis-commander](https://github.com/joeferner/redis-commander)

```shell
$ npm install -g redis-commander
## --> http://localhost:8081/
```

# devel

All devel commands have to be executed
in the root directory of *cmp*.

```shell
$ cd path/to/cmp
```

## documentation

(re)generate documentation

```shell
$ lein codox
```

## tests

```shell
$ lein test
```

## code coverage

```shell
$ lein cloverage
```

## ns-graph

```shell
$ lein ns-graph
```
