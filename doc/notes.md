
# ideas 

## cd-id

* skip load step (load always on build)
* plug in the cd-id right before send task to worker

**consequence is:**

**pro**

* the cust: true tasks vanish
* no additional tasks have to be generated at runtime

**con**

* additional cust proxy server (like anselm) becomes mandatory

## skip recipe concept

* assemble complete task at runtime (possible since the
struct of the definition don't change since
customer forking is no longer the way to go)
* prep-step --> check-step

## run definition_s_ in place

* further idea which reduces state
* makes the "reset-task"s no longer necessary
