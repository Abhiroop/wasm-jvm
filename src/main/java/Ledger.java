import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Mimics what Canton provides in production.
// In real Canton this talks to the distributed ledger.
// Here it is a dummy HashMap.
public class Ledger {

    private final Map<String, String> contracts = new HashMap<>();

    public String create(String template, String payload) {
        String cid = UUID.randomUUID().toString();
        contracts.put(cid, payload);
        System.out.println("[LEDGER] create template=" + template
            + " cid=" + cid.substring(0, 8));
        return cid;
    }

    public String fetch(String cid) {
        String payload = contracts.get(cid);
        if (payload == null)
            throw new RuntimeException("Contract not found: " + cid);
        System.out.println("[LEDGER] fetch cid=" + cid.substring(0, 8));
        return payload;
    }

    public void archive(String cid, String actor) {
        if (!contracts.containsKey(cid))
            throw new RuntimeException("Contract not found: " + cid);
        contracts.remove(cid);
        System.out.println("[LEDGER] archive cid="
            + cid.substring(0, 8) + " by=" + actor);
    }

    public int size() {
        return contracts.size();
    }
}