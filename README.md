![cmp](docs/cmp_logo.png)

A study of an interpreter handling
measurement program definitions
(**mpd**) written in [clojure](https://clojure.org/).

All of the `mp` state is kept in a [redis](https://redis.io) database.

The program api is documented at the 
[github.io](https://wactbprot.github.io/cmp/)
page.

The *cmp namespaces* are [connected as shown here](./docs/graph.png).


# Clojure

* [cheatsheet](https://clojure.org/api/cheatsheet)
* [clojure-style-guide](https://github.com/bbatsov/clojure-style-guide)
* [eastwood (linter)](https://github.com/jonase/eastwood)

# Config (Redis)

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

# Usage

User interaction with *cmp* is **REPL only** so far. The `core`
namespace is the entry point. Use the `(dir cmp.core)` to get an
overview: 

```clojure
(dir cmp.core)
;; ->mp-id
;; build-mpd
;; build-mpd-edn
;; build-mpd-ref
;; build-tasks
;; build-tasks-edn
;; check
;; clear
;; cs
;; ctrl
;; current-mp-id
;; doc-add
;; doc-del
;; ds
;; log-init!
;; log-start-repl-out!
;; log-stop-repl-out!
;; rc
;; refresh-tasks
;; sc
;; start-observe
;; status
;; stop
;; stop-observe
;; workon!
;; workon!!
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

Find a documented reference `mpd` in
[edn-format](https://github.com/edn-format/edn) 
at [resources/mpd-ref.edn](./resources/mpd-ref.edn).
This `mpd` called `"ref"` can be used as a example.
Build check and start with:


```clojure
(workon! "ref")
(build-ref-edn)
(check)
(start-observe)
(rc 0) ;; abbrev. for (ctrl 0 "run") 
```

```clojure
(workon!! mp-id)
(rc 0)
(sc 0)
```

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
