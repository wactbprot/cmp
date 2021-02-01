# documentation

* [API](./api)
* [example session](./uberdoc.html)
* [coverage](./coverage)
* [config.edn](./config.edn.html)
* [mpd-ref.edn](./mpd-ref.edn.html)

# installation
## Leiningen on Ubuntu 

```shell
sudo apt install leiningen
```

##  Leiningen on openSUSE (LEAP 15)

```shell
zypper ar https://download.opensuse.org/repositories/devel:/languages:/clojure/openSUSE_Leap_15.1/devel:languages:clojure.repo
zypper ref devel_languages_clojure
zypper in  leiningen
```

## cmp from git repo 

```shell
git clone https://github.com/wactbprot/cmp.git
cd cmp
lein deps
lein repl 
```

# Usage

User interaction with **cmp** is **REPL only**. The `core`
namespace is the entry point. Use `(dir cmp.core)` or 
take a look at [the api](./api) to get an overview. 

Since 0.20.0 the pp-table concept is dropped. The data browser 
[portal](https://github.com/djblue/portal) seems to be the
way to go.

ToDo: write a new [example session](./uberdoc.html) on how to proceed.

## portal

* https://github.com/djblue/portal

```clojure
(def portal (p/open))
(tap> (c-data))
```

# links
## Clojure

* [cheatsheet](https://clojure.org/api/cheatsheet)
* [clojure-style-guide](https://github.com/bbatsov/clojure-style-guide)
* [eastwood (linter)](https://github.com/jonase/eastwood)

## Redis

All of the `mp` state is kept in a [redis](https://redis.io) database.

### config

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

# check status
$ sudo systemctl status redis.service
```

### redis gui

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

## build example session

```shell
$ lein marg resources/example-session.clj
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

### Run tests from REPL

Example `utils-tests`:

```clojure
(ns cmp.utils-test) 
(use 'clojure.test)
(run-tests)
```

## code coverage

```shell
$ lein cloverage
```
