package com.argorice.epicysm.compat;

import java.util.List;
import java.util.function.Predicate;

/** Runtime identities shared by Yes Steve Model and OpenYSM integrations. */
public final class YsmProvider {
    public static final List<String> MOD_IDS = List.of("yes_steve_model", "openysm");
    public static final List<String> MODEL_DIRECTORIES = List.of("yes_steve_model", "openysm");
    public static final List<String> SELECTION_KEYS = List.of("yes_steve_model:model_id", "openysm:model_id");
    private static final List<String> PACKAGES = List.of("com.elfmcys.", "org.openysm.", "rip.ysm.");

    private YsmProvider() {
    }

    public static void require(Predicate<String> loaded) {
        if (MOD_IDS.stream().noneMatch(loaded)) {
            throw new IllegalStateException("EpicYSM requires Yes Steve Model (yes_steve_model) or OpenYSM (openysm)");
        }
    }

    public static boolean isRuntimeClass(Class<?> type) {
        return type != null && isRuntimeClass(type.getName());
    }

    public static boolean isRuntimeClass(String name) {
        return name != null && PACKAGES.stream().anyMatch(name::startsWith);
    }

    public static String shortClassName(String name) {
        for (String prefix : PACKAGES) {
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    public static boolean mentionedIn(String text) {
        return MOD_IDS.stream().anyMatch(text::contains);
    }
}
