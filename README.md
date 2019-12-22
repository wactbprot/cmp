![cmp](./Clojure_logo.png)

# cmp

A study of an interpreter handling
measurement-program-definitions (mpd)
written in clojure.

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```

## view documentation

[api](./docs/index.html)

```shell
$ cd path/to/cmp
$ firefox docs/index.html
```

## Usage

REPL only

```
 (workon! mp-id)
    +
    :
    v
  (clear)
    +
    :
    v
  (build)+---+
    +        :
    |        v
    =     (check-mp)
    |        +
    v        :
  (start)<---+
    +
    |
    +-->(push i "run")
    |
    +-->(status)
```
