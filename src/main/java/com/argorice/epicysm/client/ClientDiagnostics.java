package com.argorice.epicysm.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import com.argorice.epicysm.EpicYsm;
import com.argorice.epicysm.client.compat.PlayerRendererSlot;
import com.argorice.epicysm.client.ysm.YsmRenderBridge;
import com.argorice.epicysm.client.ysm.YsmSkeletonOverlay;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Observes rendering without changing model selection, poses or recovery policy. */
public final class ClientDiagnostics {
    private static DiagnosticJournal journal;
    private static boolean unavailable;
    private static long lastSample;
    private static final Map<String, Long> reported = new LinkedHashMap<>();
    private static final Map<String, JsonObject> frames = new LinkedHashMap<>();
    private static final Map<String, Long> frameTimes = new LinkedHashMap<>();

    private ClientDiagnostics() {
    }

    public static String identity(Object value) {
        return value == null ? "null" : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }

    public static JsonObject fields(Object... pairs) {
        JsonObject out = new JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            String key = String.valueOf(pairs[i]);
            if (value instanceof com.google.gson.JsonElement element) out.add(key, element.deepCopy());
            else if (value instanceof Iterable<?> items) {
                JsonArray array = new JsonArray();
                items.forEach(item -> array.add(String.valueOf(item)));
                out.add(key, array);
            }
            else if (value instanceof Boolean flag) out.addProperty(key, flag);
            else if (value instanceof Number number && Double.isFinite(number.doubleValue())) out.addProperty(key, number);
            else out.addProperty(key, String.valueOf(value));
        }
        return out;
    }

    public static JsonArray numbers(float[] array, int start, int count) {
        JsonArray out = new JsonArray();
        if (array != null && start >= 0) {
            for (int i = start; i < Math.min(array.length, start + count); i++) {
                if (Float.isFinite(array[i])) out.add(array[i]);
                else out.add(String.valueOf(array[i]));
            }
        }
        return out;
    }

    public static JsonObject entity(LivingEntity entity) {
        if (entity == null) return fields("entity", "null");
        JsonObject out = fields("entity", identity(entity), "uuid", entity.getUUID(), "tick", entity.tickCount,
                "world", identity(entity.level()), "dimension", entity.level().dimension().location(),
                "game_time", entity.level().getGameTime(), "alive", entity.isAlive(), "sleeping", entity.isSleeping());
        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            out.addProperty("patch", identity(patch));
            if (patch != null) {
                out.addProperty("animator", identity(patch.getAnimator()));
                out.addProperty("armature", identity(patch.getArmature()));
                out.addProperty("in_action", patch.getEntityState().inaction());
            }
            out.addProperty("fighting", entity instanceof net.minecraft.client.player.AbstractClientPlayer player
                    ? YsmSkeletonOverlay.fighting(player) : patch != null && patch.overrideRender());
        } catch (Throwable t) {
            out.addProperty("patch_error", t.toString());
        }
        return out;
    }

    public static void start() {
        if (journal != null || unavailable) return;
        try {
            var client = Minecraft.getInstance();
            if (client == null) return;
            Path folder = client.gameDirectory.toPath().resolve("logs/epicysm-diagnostics");
            journal = new DiagnosticJournal(folder, 160, 4 * 1024 * 1024);
            JsonArray mods = new JsonArray();
            for (String loader : new String[] { "net.minecraftforge.fml.ModList", "net.neoforged.fml.ModList" }) {
                try {
                    Object list = Class.forName(loader).getMethod("get").invoke(null);
                    for (Object mod : (Iterable<?>) list.getClass().getMethod("getMods").invoke(list)) {
                        Class<?> info = Class.forName(loader.startsWith("net.minecraftforge.")
                                ? "net.minecraftforge.forgespi.language.IModInfo" : "net.neoforged.neoforgespi.language.IModInfo");
                        mods.add(fields("id", info.getMethod("getModId").invoke(mod), "version", info.getMethod("getVersion").invoke(mod)));
                    }
                    break;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            JsonObject boot = fields("schema", 1, "java", System.getProperty("java.version"), "os", System.getProperty("os.name"));
            boot.add("mods", mods);
            journal.record("boot", boot);
            EpicYsm.LOGGER.info("Runtime diagnostics are saved to {}; /epicysm report exports a ZIP", folder);
        } catch (Throwable t) {
            unavailable = true;
            EpicYsm.LOGGER.warn("Could not start runtime diagnostics", t);
        }
    }

    public static void event(String kind, JsonObject data) {
        try {
            start();
            if (journal != null) journal.record(kind, data);
        } catch (Throwable t) {
            unavailable = true;
        }
    }

    public static JsonObject snapshot(LivingEntity subject, boolean detailed) {
        JsonObject out = fields("screen", identity(Minecraft.getInstance().screen));
        out.add("entity", entity(subject));
        out.add("manager", ModelManager.get().diagnosticState(subject));
        out.add("bridge", YsmRenderBridge.diagnosticState());
        out.add("renderer_slot", PlayerRendererSlot.diagnosticState(subject == null ? 0 : subject.tickCount));
        out.add("config", fields("skeleton_overlay", EpicYsmConfig.skeletonOverlay(), "hold", EpicYsmConfig.hold(),
                "unreadable", EpicYsmConfig.unreadableModels(), "own_animations", EpicYsmConfig.ownAnimations(),
                "physics", EpicYsmConfig.physics(), "items", EpicYsmConfig.epicFightItems(),
                "hide_weapons_open", EpicYsmConfig.hideWeaponsOpen(), "hide_weapons_locked", EpicYsmConfig.hideWeaponsLocked()));
        if (detailed && subject instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            out.add("provider_selection", fields("value", com.argorice.epicysm.client.ysm.YsmSelection.of(player)));
            try {
                Class<?> config = Class.forName("org.openysm.config.GeneralConfig", false, ClientDiagnostics.class.getClassLoader());
                JsonObject options = new JsonObject();
                for (String name : new String[] { "USE_GPU_RENDERER", "USE_COMPATIBILITY_RENDERER", "DISABLE_SELF_MODEL", "DISABLE_OTHER_MODEL" }) {
                    Object value = config.getField(name).get(null);
                    options.addProperty(name, String.valueOf(value.getClass().getMethod("get").invoke(value)));
                }
                out.add("openysm_render_options", options);
            } catch (Throwable ignored) {
            }
        }
        out.add("overlays", YsmSkeletonOverlay.diagnosticStates(detailed));
        JsonArray recent = new JsonArray();
        frames.values().forEach(f -> recent.add(f.deepCopy()));
        out.add("recent_frames", recent);
        return out;
    }

    public static void worldBoundary(Object previous, Object next) {
        try {
            JsonObject state = snapshot(Minecraft.getInstance().player, true);
            state.addProperty("previous_world", identity(previous));
            state.addProperty("next_world", identity(next));
            event("world_change", state);
        } catch (Throwable error) {
            event("world_change", fields("previous", identity(previous), "next", identity(next), "snapshot_error", error.toString()));
        }
    }

    public static void tick() {
        try {
            long now = System.nanoTime();
            if (lastSample != 0L && now - lastSample < 2_000_000_000L) return;
            lastSample = now;
            var player = Minecraft.getInstance().player;
            JsonObject state = snapshot(player, false);
            event("sample", state);
            JsonObject slot = state.getAsJsonObject("renderer_slot");
            if (slot.has("overflow_blocks_check") && slot.get("overflow_blocks_check").getAsBoolean()) {
                problem("renderer_slot_clock_overflow", player, null);
            }
        } catch (Throwable t) {
            event("snapshot_error", fields("error", t.toString()));
        }
    }

    public static void frame(LivingEntity entity, String outcome, JsonObject detail) {
        try {
            String key = entity.getUUID() + ":" + outcome;
            long now = System.nanoTime();
            if (frameTimes.containsKey(key) && now - frameTimes.get(key) < 2_000_000_000L) return;
            frameTimes.put(key, now);
            JsonObject record = fields("outcome", outcome, "observed_nanos", now);
            record.add("entity", entity(entity));
            record.add("detail", detail);
            JsonArray stack = new JsonArray();
            StackWalker.getInstance().walk(s -> s.limit(20).toList()).forEach(f -> stack.add(f.getClassName() + "." + f.getMethodName()));
            record.add("stack", stack);
            frames.put(key, record);
            while (frames.size() > 24) {
                String oldest = frames.keySet().iterator().next();
                frames.remove(oldest);
                frameTimes.remove(oldest);
            }
            event("render_frame", record);
        } catch (Throwable ignored) {
        }
    }

    public static void problem(String reason, LivingEntity entity, Throwable error) {
        try {
            long now = System.nanoTime();
            String key = reason + ":" + identity(entity) + ":" + (entity == null ? "null" : identity(entity.level()));
            if (reported.containsKey(key)) return;
            reported.put(key, now);
            while (reported.size() > 128) reported.remove(reported.keySet().iterator().next());
            start();
            if (journal == null) return;
            JsonObject state = snapshot(entity, true);
            if (error != null) {
                StringWriter trace = new StringWriter();
                error.printStackTrace(new PrintWriter(trace));
                state.addProperty("exception", trace.toString());
            }
            Path file = journal.incident(reason, state);
            EpicYsm.LOGGER.warn("Runtime diagnostic incident {} saved to {}", reason, file);
        } catch (Throwable t) {
            EpicYsm.LOGGER.debug("Could not write diagnostic incident", t);
        }
    }

    public static Path export() throws Exception {
        start();
        if (journal == null) throw new java.io.IOException("Runtime diagnostics are unavailable");
        return journal.export(snapshot(Minecraft.getInstance().player, true));
    }
}
