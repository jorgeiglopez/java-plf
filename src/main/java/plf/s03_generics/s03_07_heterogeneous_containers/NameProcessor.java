package plf.s03_generics.s03_07_heterogeneous_containers;

import java.util.List;

public final class NameProcessor {

    private NameProcessor() {
    }

    public static int totalLength(TypesafeMap registry) {
        List<String> names = registry.get(List.class);
        int total = 0;
        for (String name : names) {
            total += name.length();
        }
        registry.put(List.class, List.of(1, 2, 3));
        return total;
    }
}
