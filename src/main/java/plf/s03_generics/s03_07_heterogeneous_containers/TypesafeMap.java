package plf.s03_generics.s03_07_heterogeneous_containers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TypesafeMap {

    private final Map<Class<?>, Object> store = new HashMap<>();

    public <T> void put(Class<T> type, T instance) {
        store.put(Objects.requireNonNull(type), instance);
    }

    public <T> T get(Class<T> type) {
        return type.cast(store.get(type));
    }
}
