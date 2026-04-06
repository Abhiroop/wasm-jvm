import java.util.List;
import java.util.ArrayList;

public class Benchmark {

    public static void main(String[] args) {

        int warmup = 50;
        int runs   = 200;

        // ── warmup ───────────────────────────────────────────────────────
        System.out.println("Warming up...");
        for (int i = 0; i < warmup; i++) {
            runLifecycle();
        }
        System.out.println("Warmup done (" + warmup + " iterations)\n");

        // ── benchmark full lifecycle ──────────────────────────────────────
        long t0 = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            runLifecycle();
        }
        long totalNs = System.nanoTime() - t0;

        // ── results ──────────────────────────────────────────────────────
        double totalMs   = totalNs / 1_000_000.0;
        double perCallMs = totalMs / runs;
        double perSec    = runs / (totalNs / 1_000_000_000.0);

        System.out.printf("%-12s  %10s  %10s  %12s%n",
            "Operation", "Total", "Per call", "Per sec");
        System.out.println("-".repeat(50));
        System.out.printf("%-12s  %8.1fms  %8.3fms  %12.0f%n",
            "lifecycle", totalMs, perCallMs, perSec);

        System.out.println();
        System.out.println("Note: each lifecycle = create + transfer + redeem");
        System.out.println("Note: includes Chicory instantiation overhead per run");
        System.out.println();
        System.out.printf("Chicory lifecycle:            %.3fms%n", perCallMs);
    }

    static void runLifecycle() {
        var ledger  = new Ledger();
        var runtime = new WasmRuntime("./iou.wasm", ledger);

        String cid = runtime.callForCid("iou_create",
            "{\"issuer\":\"Alice\",\"owner\":\"Bob\",\"amount\":\"100\"}");

        String newCid = runtime.callForCid("iou_transfer",
            "{\"cid\":\"" + cid + "\","
          + "\"new_owner\":\"Charlie\","
          + "\"actor\":\"Bob\"}");

        runtime.callVoid("iou_redeem",
            "{\"cid\":\"" + newCid + "\",\"actor\":\"Charlie\"}");
    }
}