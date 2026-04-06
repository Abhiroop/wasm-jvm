public class Main {
    public static void main(String[] args) {

        var ledger  = new Ledger();
        var runtime = new WasmRuntime("./iou.wasm", ledger);

        // create
        System.out.println("\n--- Creating IOU ---");
        String cid = runtime.callForCid("iou_create",
            "{\"issuer\":\"Alice\",\"owner\":\"Bob\",\"amount\":\"100\"}");
        System.out.println("cid=" + cid.substring(0, 8));

        // transfer
        System.out.println("\n--- Transferring to Charlie ---");
        String newCid = runtime.callForCid("iou_transfer",
            "{\"cid\":\"" + cid + "\","
          + "\"new_owner\":\"Charlie\","
          + "\"actor\":\"Bob\"}");
        System.out.println("newCid=" + newCid.substring(0, 8));

        // redeem
        System.out.println("\n--- Redeeming ---");
        runtime.callVoid("iou_redeem",
            "{\"cid\":\"" + newCid + "\","
          + "\"actor\":\"Charlie\"}");

        System.out.println("\nFinal ledger size: " + ledger.size());
    }
}