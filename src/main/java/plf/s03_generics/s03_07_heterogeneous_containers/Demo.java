package plf.s03_generics.s03_07_heterogeneous_containers;

import java.util.List;

public final class Demo {

    public static void main(String[] args) {
        TypesafeMap registry = new TypesafeMap();
        registry.put(List.class, List.of("alpha", "beta", "gamma"));

        NameProcessor.totalLength(registry);   // <-- black box; you cannot see inside

        List<String> names = registry.get(List.class);
        names.forEach(name -> System.out.println(name.toUpperCase()));   // <-- predict this
    }
}
