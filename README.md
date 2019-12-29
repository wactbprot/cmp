![cmp](./Clojure_logo.png)

# cmp

A study of an interpreter handling
measurement-program-definitions (mpd)
written in clojure.

See the [documentation on github.io pages.](https://wactbprot.github.io/cmp/)


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
