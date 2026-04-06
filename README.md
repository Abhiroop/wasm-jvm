## wasm-jvm

A demonstration of running WASM smart contracts on the JVM using  [Chicory](https://chicory.dev/) — a pure Java WASM interpreter with 
zero native dependencies.

## Running

Build and run this project

```
make run
```

Dependencies:
- maven
- jdk 21 onwards
- rust and cargo toolchain

Have these installed and available on PATH and `make run` is all you need.


### Whats going on?

The example IOU contract, written in Rust, is found [here](./iou-contract/src/lib.rs). Once this gets compiled down to WASM, the [Chicory WASM interpreter](https://chicory.dev/) is used as a Java library to interpret the compiled WASM and operate on a mock ledger. The [ledger](./src/main/java/Ledger.java) is modeled as a dummy map. 

The workflow looks like
```
Rust contract  ->  WASM binary  ->  Chicory (JVM library)  ->  Ledger (Java)
```

The following ledger API calls are available to the contract:


```java
String create(String template, String payload, String signatories);
String fetch(String contractId);
void   archive(String contractId, String actor);
```

Owing to WASM's limited type system, the types have been represented as low-level messages in the form of a pointer and message length pair ([the low-level API](./iou-contract/src/ledger.rs)).

The execution of the contract is done [here](./src/main/java/Main.java) and is also the entry point. The hooking up of chicory to your JVM app allowing calls to WASM are abstracted in this [module](./src/main/java/WasmRuntime.java).

### Project structure
```
iou-contract/        Rust WASM contract
  src/lib.rs         IOU contract logic (create, transfer, redeem)
  src/ledger.rs      Canton API declarations (implemented by Java)
  src/memory.rs      WASM/JVM memory bridge (plumbing)

src/main/java/
  Main.java          Entry point — runs the contract lifecycle
  Ledger.java        Mock Canton ledger (HashMap)
  WasmRuntime.java   Chicory wiring — loads WASM, registers host functions
```

### Expected Output

```
--- Creating IOU ---
[LEDGER] create template=Iou cid=add6a682
cid=add6a682

--- Transferring to Charlie ---
[LEDGER] fetch cid=add6a682
[LEDGER] archive cid=add6a682 by=Bob
[LEDGER] create template=Iou cid=6b0a7ce4
newCid=6b0a7ce4

--- Redeeming ---
[LEDGER] fetch cid=6b0a7ce4
[LEDGER] archive cid=6b0a7ce4 by=Charlie

Final ledger size: 0
```

### Benchmark

```
Operation          Total    Per call       Per sec
--------------------------------------------------
lifecycle       1023.9ms     5.120ms           195

Note: each lifecycle = create + transfer + redeem
Note: includes Chicory instantiation overhead per run

Chicory lifecycle:            5.120ms
```

One lifecyle includes 9 boundary crossings:

```
iou_create called from Java          → 1 crossing in
    canton_create called from WASM   → 1 crossing out
iou_transfer called from Java        → 1 crossing in
    canton_fetch called from WASM    → 1 crossing out
    canton_archive called from WASM  → 1 crossing out
    canton_create called from WASM   → 1 crossing out
iou_redeem called from Java          → 1 crossing in
    canton_fetch called from WASM    → 1 crossing out
    canton_archive called from WASM  → 1 crossing out
```

All 9 crossings combined plus GC pause, operation overhead etc takes 5 ms.