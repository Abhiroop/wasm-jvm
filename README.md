## wasm-jvm

Build and run this project

```
make run
```

Dependencies:
- maven
- jdk 21 onwards
- rust and cargo toolchain

Have these installed and available on PATH and `make run` is all you need.


#### Whats going on?

The example IOU contract, written in Rust, is found [here](./iou-contract/src/lib.rs). Once this gets compiled down to WASM, the [Chicory WASM interpreter](https://chicory.dev/) is used as a Java library to interpret the compiled WASM and operate on a mock ledger. The [ledger](./src/main/java/Ledger.java) is modeled as a dummy map. The following ledger API calls are available to the contract:


```java
String create(String template, String payload, String signatories);
String fetch(String contractId);
void   archive(String contractId, String actor);
```

Owing to WASM's limited type system, they types have been represented as low-level messages in the form of a pointer and message length pair ([the low-level API](./iou-contract/src/ledger.rs)).

The execution of the contract is done [here](./src/main/java/Main.java) and is also the entry point. The hooking up of chicory to allow message transfer and foreign-function calls to WASM are abstracted in this [runtime module](./src/main/java/WasmRuntime.java)