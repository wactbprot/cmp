# cmp

A study of an interpreter for measurement program definitions (mpd) written in clojure.

## Usage

repl only (so far)

## poll

```clojure
cmp.poll> (ns cmp.poll)
cmp.poll> (start "se3-calib" 5)
{"se3-calib@container@5@ctrl" #<Future@58754b: :pending>}void
void
void
... (set ctrl to run)-->
(se3-calib@container@5@state@1@0)
runing
runing
...
cmp.poll> (stop "se3-calib" 5)
```
