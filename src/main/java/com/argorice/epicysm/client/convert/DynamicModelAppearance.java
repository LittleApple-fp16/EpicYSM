package com.argorice.epicysm.client.convert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Identifies scale channels whose appearance must be evaluated by YSM at render time. */
public final class DynamicModelAppearance {
    private DynamicModelAppearance() {
    }

    public static boolean needsLiveRendering(List<JsonObject> animationFiles, List<JsonObject> controllerFiles) {
        Map<String, Integer> fixedScales = new HashMap<>();

        for (JsonObject file : animationFiles) {
            if (!file.has("animations") || !file.get("animations").isJsonObject()) {
                continue;
            }

            for (var animation : file.getAsJsonObject("animations").entrySet()) {
                if (!animation.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject definition = animation.getValue().getAsJsonObject();
                if (!definition.has("bones") || !definition.get("bones").isJsonObject()) {
                    continue;
                }

                boolean alwaysPlaying = animation.getKey().startsWith("parallel")
                        || animation.getKey().startsWith("pre_parallel");

                for (var bone : definition.getAsJsonObject("bones").entrySet()) {
                    if (!bone.getValue().isJsonObject() || !bone.getValue().getAsJsonObject().has("scale")) {
                        continue;
                    }

                    int scale = fixedVisibility(bone.getValue().getAsJsonObject().get("scale"));
                    if (!alwaysPlaying || scale < 0) {
                        return true;
                    }

                    Integer previous = fixedScales.putIfAbsent(bone.getKey(), scale);
                    if (previous != null && previous != scale) {
                        return true;
                    }
                }
            }
        }

        return !fixedScales.isEmpty() && controllerFiles.stream()
                .anyMatch(file -> file.has("animation_controllers"));
    }

    /** Only an unconditional identity scale or a completely hidden bone can be baked safely. */
    private static int fixedVisibility(JsonElement value) {
        if (value.isJsonPrimitive()) {
            try {
                float scale = Float.parseFloat(value.getAsString());
                return scale == 0.0F ? 0 : scale == 1.0F ? 1 : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        if (value.isJsonArray() && value.getAsJsonArray().size() == 3) {
            int first = fixedVisibility(value.getAsJsonArray().get(0));
            return first >= 0 && fixedVisibility(value.getAsJsonArray().get(1)) == first
                    && fixedVisibility(value.getAsJsonArray().get(2)) == first ? first : -1;
        }

        return -1;
    }
}
