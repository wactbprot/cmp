![cmp](docs/cmp_logo.png)

A study of an interpreter, handling
measurement-program (**mp**) definitions
(**mpd**) written in [clojure](https://clojure.org/).

All of the `mp` state is kept in *redis*
(and only there).

**Hence:**
* GUIs and other helpers just interact with the *redis key-value store* 
* a stateful `mp` (a `mp` in time) should be portable to a
different machine by porting the database and starting *cmp*.
* lots of *redis* apps are useful in an undiscovered way

See the [cmp documentation on github.io.](https://wactbprot.github.io/cmp/)

## config redis

![redis](docs/redis_logo.png)

Since version 0.3.0 *cmp* relies on
[Keyspace Notifications](https://redis.io/topics/notifications).
Therefore it is necassary to swap:

```shell
notify-keyspace-events ""
```

by


```shell
notify-keyspace-events AK

```

in the file `/etc/redis/redis.conf` and restart the service:


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

## Usage

![redis](docs/clojure_logo.png)

**REPL only**

```
 (workon! mp-id)
    _
    |
    v
  (clear)
    _
    |
    v
  (build)
    _
    |
    v
  (check)
    _
    |
    v
  (start)
    _
    |
    +-->(ctrl! 0 "run")
    |
    |
    +-->(status)
```

Same as:

```
 (workon!! mp-id)
    _
    |
    +-->(ctrl! 0 "run")
    |
    |
    +-->(status 0)
```

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
