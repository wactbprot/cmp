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

## arch:
### idea --> cd-id:

- skip load step (load always on build)
- attache the cd-id right before send task to worker

### consequence is:

#### pro:
- the cust: true tasks vanish
- no additional tasks have to be generated at runtime

#### con:
- additional cust proxy server (like anselm) becomes mandatory

### idea --> skip recipe concept

- assemble complete task at runtime (possible since the
struct of the definition don't change since
customer forking is no longer the way to go) 
- prep-step --> check-step

### idea --> run definition_s_ in place

- further idea which reduces state
- makes the "reset-bla-task"s no longer necessary 

## redis gui

### redis-commander

$ npm install -g redis-commander

https://github.com/joeferner/redis-commander
http://localhost:8081/
