# glossary

* listeners ... functions reacting on events occuring for certain key spaces
* mp ... measurement program
* mpd ... measurement program definition (see [[mpd-ref.edn]]

# examples

For the `curl` examples, the *environment variable*:

```shell
export H="Content-Type: application/json"
```
is helpful. It is used as follows:

```shell
curl -H "$H" ...
```


## listeners

Returns a vector of objects containing the currently registered
listeners. A registered listener is necessary for a running mp.

```shell
curl -H "$H" -X GET http://localhost:8010/listeners
## =>
## [{:mp-id "ref",
##   :struct "*",
##   :no-idx "*",
##   :func "ctrl",
##   :level "a",
##   :listener-id "82f57e39-6eac-4d73-b0d2-b913403a6d60"}]
```

## tasks

Returns a vector of objects containing the key-value pairs of all tasks available for cmp.

```shell
curl -H "$H" -X GET http://localhost:8010/listeners
## =>
## [{:key "tasks@1000T_4038-ctrl_pfill",
##   :value
##  {
##    "Port": "%port",
##    "NoLog": true,
##    "StopIf": "Filling_Pressure_Ok.Ready",
##    "TaskName": "1000T_4038-ctrl_pfill",
##    "Comment": "Berechnet die aktuelle Abweichung des p_ist vom p_soll und daraus den setpoint für den flow controller",
##    "Repeat": "3",
##    "Fallback": {
##      "ToExchange": {
##        "Filling_Pressure_Ok.Ready": false
##      }
##    },
##	...]
```

## container

There are several endpoints working in the same way as the following.

Returns a vector of objects containing the key-value pairs of all `mp` (here `ref`) container titles.

```shell
curl -H "$H" -X GET http://localhost:8010/ref/container/title
##=>
##[
##  {
##    "key": "ref@container@000@title",
##    "value": "multiple wait tasks"
##  },
##  ...]
```

```shell
curl -H "$H" -X GET http://localhost:8010/ref/container/descr
```

```shell
curl -H "$H" -X GET http://localhost:8010/ref/container/ctrl
```

```shell
curl -H "$H" -X GET http://localhost:8010/ref/container/elem
```
