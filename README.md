![cmp](./Clojure_logo.png)

# cmp

A study of an interpreter handling
measurement-program-definitions (**mpd**)
written in *clojure*. The complete
state is kept in a **redis** database. 

See the [documentation on github.io pages.](https://wactbprot.github.io/cmp/)

## config redis

In `/etc/redis/redis.conf`:

```shell
notify-keyspace-events AK
```

and restart

```shell
sudo systemctl restart redis.service
```

## redis gui

* redis-commander

```shell
$ npm install -g redis-commander
```

https://github.com/joeferner/redis-commander
http://localhost:8081/

## Usage

REPL only

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

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
