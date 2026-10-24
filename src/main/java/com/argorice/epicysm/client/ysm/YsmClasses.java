package com.argorice.epicysm.client.ysm;

import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;

import com.argorice.epicysm.EpicYsm;
import com.argorice.epicysm.compat.YsmProvider;

/** Lists provider classes through loader scan data or the renderer's module. */
final class YsmClasses {
    private static List<String> names;

    private YsmClasses() {
    }

    static synchronized List<String> names(Object sample) {
        if (names != null) {
            return names;
        }
        List<String> found = fromScan(null);
        String route = "scan";
        if (found.isEmpty()) {
            found = fromModule(sample);
            route = "module";
        }
        if (found.isEmpty()) {
            EpicYsm.LOGGER.warn("YSM provider classes could not be listed; its live model cannot be found yet");
            return List.of();
        }
        EpicYsm.LOGGER.info("YSM provider: {} classes known ({})", found.size(), route);
        names = List.copyOf(found);
        return names;
    }

    public static List<String> extending(String parentClassName) {
        return fromScan(parentClassName);
    }

    private static List<String> fromScan(String parentClassName) {
        Set<String> found = new LinkedHashSet<>();
        for (String modId : YsmProvider.MOD_IDS) {
            try {
                var modFile = ModList.get().getModFileById(modId);
                ModFileScanData scan = modFile == null ? null : modFile.getFile().getScanResult();
                if (scan == null) {
                    continue;
                }
                for (ModFileScanData.ClassData data : scan.getClasses()) {
                    String name = typeNameOf(data, "clazz", "getClazz");
                    if (YsmProvider.isRuntimeClass(name) && (parentClassName == null
                            || parentClassName.equals(typeNameOf(data, "parent", "getParent")))) {
                        found.add(name);
                    }
                }
            } catch (Throwable t) {
                EpicYsm.LOGGER.debug("Scan data unavailable for {}", modId, t);
            }
        }
        return new ArrayList<>(found);
    }

    private static String typeNameOf(ModFileScanData.ClassData data, String... accessors) {
        for (String accessor : accessors) {
            try {
                Object type = data.getClass().getMethod(accessor).invoke(data);
                if (type instanceof org.objectweb.asm.Type asmType) {
                    return asmType.getClassName();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static List<String> fromModule(Object sample) {
        List<String> found = new ArrayList<>();
        try {
            if (sample == null) {
                return found;
            }
            Module module = sample.getClass().getModule();
            if (!module.isNamed() || module.getLayer() == null) {
                return found;
            }
            Optional<ResolvedModule> resolved = module.getLayer().configuration().findModule(module.getName());
            if (resolved.isEmpty()) {
                return found;
            }
            try (ModuleReader reader = resolved.get().reference().open(); Stream<String> entries = reader.list()) {
                for (String entry : (Iterable<String>) entries::iterator) {
                    if (entry.endsWith(".class") && !entry.equals("module-info.class")) {
                        String name = entry.substring(0, entry.length() - 6).replace('/', '.');
                        if (YsmProvider.isRuntimeClass(name)) {
                            found.add(name);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            EpicYsm.LOGGER.debug("YSM provider module unavailable", t);
        }
        return found;
    }
}
