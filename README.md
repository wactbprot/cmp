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
# Tests and code coverage

```shell
lein test
```

```shell
lein cloverage
```


# Usage

User interaction with *cmp* is **REPL only** so far. The `core`
namespace is the entry point. Use the `(dir cmp.core)` to get an
overview: 

```clojure
;;   (dir cmp.core)
;; ->mp-id
;; current-mp-id
;;
;; build-mpd
;; build-mpd-edn
;;
;; check
;;
;; clear
;; clear-all

;;
;; doc-add
;; doc-del
;;
;; log-start-repl-out!
;; log-stop-repl-out!
;;
;; build-task-edn
;; build-tasks
;; refresh-tasks
;;
;; reset-c
;; run-c
;; stop-c
;; set-ctrl
;;
;; stat-c
;; stat-d
;;
;; start-observe
;; stop-observe
;;
;; workon!
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
(run-c 0) ;; abbrev. for (ctrl 0 "run") 
```

```clojure
(workon!! mp-id)
(run-c 0)
(stat-c 0)
```

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
