![cmp](./Clojure_logo.png)

# cmp

A study of an interpreter handling
measurement-program-definitions (mpd)
written in clojure.

See the [documentation on github pages.](https://wactbprot.github.io/cmp/)


## Usage

REPL only

```
 (workon! mp-id)
    +
    |
    v
  (clear)
    +
    |
    v
  (build)
    +
    |
  (check-mp)
    |
    v
  (start)
    +
    |
    +-->(push 0 "run")
    |
    +-->(status)
```

## (re)generate documentation

```shell
$ cd path/to/cmp
$ lein codox
```
