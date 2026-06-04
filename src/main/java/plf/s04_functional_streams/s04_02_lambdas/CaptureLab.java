package plf.s04_functional_streams.s04_02_lambdas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class CaptureLab {

    private CaptureLab() {
    }

    public static List<Supplier<String>> buildSuppliers(List<String> seeds) {
        List<Supplier<String>> out = new ArrayList<>();
        for (String seed : seeds) {
            StringBuilder sb = new StringBuilder(seed);
            out.add(() -> sb.reverse().toString());
        }
        return out;
    }

    public static List<String> runPass(List<Supplier<String>> suppliers) {
        List<String> results = new ArrayList<>();
        for (Supplier<String> s : suppliers) {
            results.add(s.get());
        }
        return results;
    }

    public static void main(String[] args) {
        List<Supplier<String>> suppliers = buildSuppliers(List.of("ab", "cd", "ef"));
        System.out.println("pass 1: " + runPass(suppliers));
        System.out.println("pass 2: " + runPass(suppliers));
    }
}
