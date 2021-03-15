![cmp](docs/cmp_logo.png)


A study of an interpreter handling
measurement program definitions
(**mpd**) written in [clojure](https://clojure.org/).

## Environment variables

* `CMP_LT_SRV` ... CouchDB server Bsp.:  `export CMP_LT_SRV="127.0.0.1"` or `export CMP_LT_SRV="a73434"`
* `CMP_BUILD_ON_START` ... mpds to build on Start Bsp.:  `export CMP_BUILD_ON_START="ppc-gas_dosing"` or `export CMP_BUILD_ON_START="se3-servo,se3-cmp_valves,se3-cmp_state"`
* `CMP_DEVHUB_URL` ... url for device requests (Action: TCP, VXI11, MODBUS EXECUTE) Bsp.: `export CMP_DEVHUB_URL="http://localhost:9009"` or `export CMP_DEVHUB_URL="http://a73434:55555"`

See documentation on [wactbprot.github.io](https://wactbprot.github.io/cmp/)
