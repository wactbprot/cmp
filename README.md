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

User interaction with *cmp* is **REPL only** so far.


Find a documented reference `mpd` in
[edn-format](https://github.com/edn-format/edn) 
at [resources/mpd-ref.edn](./resources/mpd-ref.edn).
This `mpd` called `"ref"` can be used as a example.
Build check and start with:


```clojure
(build-ref)
(workon! "ref")
(check)
(start)
(ctrl! 0 "run")
```

```Access log
;; ... :48 hiob INFO [cmp.core:187] - push cmd to: ref
;; ... :48 hiob INFO [cmp.core:190] - done  [ ref ]
;; ... :48 hiob DEBUG [cmp.state:361] - receive key  ref@container@0@ctrl and start
;; ... :48 hiob INFO [cmp.reg:43] - registered listener for:  ref container 0 state
;; ... :48 hiob INFO [cmp.st-mem:103] - subscribed to  __keyspace@0*__:ref@container@0@state*
;; ... :48 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@0@0  try to get task and call worker
;; ... :48 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :48 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@0@1  try to get task and call worker
;; ... :48 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :51 hiob INFO [cmp.worker.wait:20] - wait time ( 3000 ms) over for  ref@container@0@state@0@0
;; ... :52 hiob INFO [cmp.worker.wait:20] - wait time ( 4000 ms) over for  ref@container@0@state@0@1
;; ... :53 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@1@0  try to get task and call worker
;; ... :53 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :53 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@1@1  try to get task and call worker
;; ... :53 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :53 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@1@2  try to get task and call worker
;; ... :53 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :53 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@1@3  try to get task and call worker
;; ... :53 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :56 hiob INFO [cmp.worker.wait:20] - wait time ( 3000 ms) over for  ref@container@0@state@1@0
;; ... :56 hiob INFO [cmp.worker.wait:20] - wait time ( 3000 ms) over for  ref@container@0@state@1@1
;; ... :56 hiob INFO [cmp.worker.wait:20] - wait time ( 3000 ms) over for  ref@container@0@state@1@2
;; ... :56 hiob INFO [cmp.worker.wait:20] - wait time ( 3000 ms) over for  ref@container@0@state@1@3
;; ... :57 hiob DEBUG [cmp.work:62] - receive key  ref@container@0@definition@2@0  try to get task and call worker
;; ... :57 hiob DEBUG [cmp.lt-mem:20] - get task:  Common-wait  from ltm
;; ... :58 hiob INFO [cmp.worker.wait:20] - wait time ( 1000 ms) over for  ref@container@0@state@2@0
;; ... :58 hiob INFO [cmp.state:275] - all done at  ref@container@0@ctrl  (run branch)
;; ... :58 hiob DEBUG [cmp.ctrl:34] - dispatch default branch for key:  ref@container@0@ctrl
;; ... :58 hiob INFO [cmp.reg:54] - de-registered listener:  ref_container_0_state

```

```clojure
(workon! mp-id)
(clear)
(build)
(check)
(start)
;; followed by
(ctrl! 0 "run")
(status)
```

Same as:

```clojure
(workon!! mp-id)
(ctrl! 0 "run")
(status)
```

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
