import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        var module = Parser.parse(new File("./factorial.wasm"));
        Instance instance = Instance.builder(module).build();

        ExportFunction iterFact = instance.export("iterFact");

        var result = iterFact.apply(5)[0];
        System.out.println("Result: " + result);
    }
}
