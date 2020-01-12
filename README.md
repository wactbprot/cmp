![cmp](docs/cmp_logo.png)

A study of an interpreter handling
measurement program definitions
(**mpd**) written in [clojure](https://clojure.org/).

All of the `mp` state is kept in a **redis** database.

The program api is documented at the 
[github.io](https://wactbprot.github.io/cmp/)
page.

![redis](docs/redis_logo.png)

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


![clojure](docs/clojure_logo.png)

Visit a documented reference `mpd` at
[resources/ref-mpd.edn](./resources/ref-mpd.edn).
This `mpd` can be used as a example by:

```clojure
(build-ref)
```

**REPL only**

```clojure
(workon! mp-id)
;   _
;   |
;   v
(clear)
;   _
;   |
;   v
(build)
;   _
;   |
;   v
(check)
;   _
;   |
;   v
(start)
;   _
;   |
;   v
(ctrl! 0 "run")
;   :
;   :
;   :
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
