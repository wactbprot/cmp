# glossary

* listeners ... functions reacting on events occuring for certain key spaces

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

Returns a vector of objects containing the registered  listeners.

```shell
curl -H "$H" -X GET http://localhost:8010/listeners
## =>
## [{"reg-key":"ref@*@*@ctrl@a","id":"82f57e39-6eac-4d73-b0d2-b913403a6d60"}]
```
