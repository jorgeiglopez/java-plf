package plf.s01_core_language.s01_01_primitives_and_wrappers;

import java.util.HashMap;
import java.util.Map;

public final class SilentUnboxing {

    private SilentUnboxing() {
    }

    public static String silentWrong() {
        Map<Long, String> byId = new HashMap<>();
        byId.put(1L, "one");
        return byId.get(1);
    }

    public static int loudDead() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("seen", 3);
        int n = counts.get("absent");
        return n;
    }

    public static void main(String[] args) {
        System.out.println("silentWrong() = " + silentWrong());
        System.out.println("loudDead()    = " + loudDead());
    }
}
