import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.FunctionType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Loads a WASM contract and wires it to a Ledger.
// This is the layer that would be provided by an SDK.

public class WasmRuntime {

    private final Instance instance;

    public WasmRuntime(String wasmPath, Ledger ledger) {
        var createFn = new HostFunction(
            "env", "canton_create",
            FunctionType.of(
                List.of(ValType.I32, ValType.I32,
                        ValType.I32, ValType.I32,
                        ValType.I32, ValType.I32),
                List.of(ValType.I64)
            ),
            (inst, args) -> {
                String template = readString((int) args[0], (int) args[1]);
                String payload  = readString((int) args[2], (int) args[3]);
                String cid      = ledger.create(template, payload);
                int ptr         = writeString(cid);
                return new long[]{ pack(ptr, cid.length()) };
            }
        );

        var fetchFn = new HostFunction(
            "env", "canton_fetch",
            FunctionType.of(
                List.of(ValType.I32, ValType.I32),
                List.of(ValType.I64)
            ),
            (inst, args) -> {
                String cid     = readString((int) args[0], (int) args[1]);
                String payload = ledger.fetch(cid);
                int ptr        = writeString(payload);
                return new long[]{ pack(ptr, payload.length()) };
            }
        );

        var archiveFn = new HostFunction(
            "env", "canton_archive",
            FunctionType.of(
                List.of(ValType.I32, ValType.I32,
                        ValType.I32, ValType.I32),
                List.of()
            ),
            (inst, args) -> {
                String cid   = readString((int) args[0], (int) args[1]);
                String actor = readString((int) args[2], (int) args[3]);
                ledger.archive(cid, actor);
                return new long[]{};
            }
        );

        instance = new Store()
            .addFunction(createFn)
            .addFunction(fetchFn)
            .addFunction(archiveFn)
            .instantiate("iou", Parser.parse(new File(wasmPath)));
    }

    // Call a contract function that returns a contract ID
    public String callForCid(String fn, String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        int ptr      = writeString(msg);
        long packed  = instance.export(fn).apply(ptr, bytes.length)[0];
        int[] pl     = unpack(packed);
        return readString(pl[0], pl[1]);
    }

    // Call a contract function that returns nothing
    public void callVoid(String fn, String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        int ptr      = writeString(msg);
        instance.export(fn).apply(ptr, bytes.length);
    }

    // ── memory helpers ────────────────────────────────────────────────────

    private String readString(int ptr, int len) {
        return instance.memory().readString(ptr, len);
    }

    private int writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int ptr = (int) instance.export("alloc").apply(bytes.length)[0];
        instance.memory().write(ptr, bytes);
        return ptr;
    }

    private long pack(int ptr, int len) {
        return ((long) ptr << 32) | (len & 0xFFFFFFFFL);
    }

    private int[] unpack(long packed) {
        return new int[]{ (int)(packed >> 32), (int)(packed & 0xFFFFFFFFL) };
    }
}