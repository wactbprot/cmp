![cmp](docs/cmp_logo.png)


An interpreter handling measurement program definitions
(**mpd**) written in [clojure](https://clojure.org/).

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [documentation](#documentation)
- [requirements](#requirements)
- [installation](#installation)
    - [redis config](#redis-config)
    - [cmp standalone version](#cmp-standalone-version)
        - [environment variables](#environment-variables)
    - [cmp REPL version](#cmp-repl-version)
        - [Leiningen on Ubuntu](#leiningen-on-ubuntu)
        - [Leiningen on openSUSE (LEAP 15)](#leiningen-on-opensuse-leap-15)
        - [cmp from git repo](#cmp-from-git-repo)
- [links](#links)
    - [Clojure](#clojure)
    - [redis guis](#redis-guis)

<!-- markdown-toc end -->

# documentation

* [API](./docs/api)
* [coverage](./docs/coverage)
* [config.edn](./resources/config.edn.html)

# requirements

* java (8 or 11)
* redis (with activated keyspace notification)

# installation 

All of the `mp` state is kept in a [redis](https://redis.io) database.

## redis config

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
$ sudo systemctl restart redis.service

# check status
$ sudo systemctl status redis.service
```

## cmp standalone version

Download the latest version from [here](http://a75438).
Set the environment variables (see next section). Start `cmp` with:

```shell
java -jar cmp-x.y.z-standalone.jar
```

### environment variables

| `var`                	| Description                                                      	| Example                                                                                                              	|   	|   	|
|----------------------	|------------------------------------------------------------------	|----------------------------------------------------------------------------------------------------------------------	|---	|---	|
| `CMP_BUILD_ON_START` 	| mpds to build on server start                                    	| `export CMP_BUILD_ON_START="ppc-gas_dosing"`<br>`export CMP_BUILD_ON_START="se3-servo,se3-cmp_valves,se3-cmp_state"` 	|   	|   	|
| `CMP_DEVHUB_URL`     	| url for device requests <br>(Action: TCP, VXI11, MODBUS EXECUTE) 	| `export CMP_DEVHUB_URL="http://localhost:9009"`<br>`export CMP_DEVHUB_URL="http://a73434:55555"`                     	|   	|   	|
| `CMP_LT_SRV`         	| CouchDB server                                                   	| `export CMP_LT_SRV="127.0.0.1"`<br>`export CMP_LT_SRV="a73434"`                                                      	|   	|   	|

## cmp REPL version

### Leiningen on Ubuntu 

```shell
sudo apt install leiningen
```

###  Leiningen on openSUSE (LEAP 15)

```shell
zypper ar https://download.opensuse.org/repositories/devel:/languages:/clojure/openSUSE_Leap_15.1/devel:languages:clojure.repo
zypper ref devel_languages_clojure
zypper in  leiningen
```

### cmp from git repo 

```shell
git clone https://github.com/wactbprot/cmp.git
cd cmp
lein deps
lein repl 
```

# links
## Clojure

* [cheatsheet](https://clojure.org/api/cheatsheet)
* [clojure-style-guide](https://github.com/bbatsov/clojure-style-guide)
* [eastwood (linter)](https://github.com/jonase/eastwood)

## redis guis

* [RedisDesktopManager](https://github.com/uglide/RedisDesktopManager)
  `sudo snap install redis-desktop-manager`
* [redis-commander](https://github.com/joeferner/redis-commander)

```shell
$ npm install -g redis-commander
## --> http://localhost:8081/
```
