package com.argorice.epicysm.client.convert;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicModelAppearanceTest {
    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static boolean live(String animations) {
        return DynamicModelAppearance.needsLiveRendering(List.of(json(animations)), List.of());
    }

    @Test
    void kipfelOutfitAndEquipmentConditionsKeepLiveAppearance() {
        assertTrue(live("""
                {"animations":{"pre_parallel0":{"bones":{
                  "research_jacket":{"scale":"((v.roaming.outfit ?? 0)==2 && (v.roaming.research_outer ?? 1) && (!(v.roaming.equipment_clothing ?? 0) || ysm.has_chest_plate))"},
                  "shell_skirt":{"scale":"(v.roaming.outfit ?? 0)==4"}
                }}}}
                """));
    }

    @Test
    void unsupportedExpressionsAreNeverBakedAsVisible() {
        assertTrue(live("""
                {"animations":{"parallel":{"bones":{"hat":{"scale":"ysm.custom_visibility(v.hat);"}}}}}
                """));
    }

    @Test
    void fixedParallelVisibilityCanStillBeConverted() {
        assertFalse(live("""
                {"animations":{"parallel":{"bones":{
                  "prop":{"scale":[0,0,0]},"body":{"scale":"1.0"}
                }}}}
                """));
    }

    @Test
    void keyframedVisibilityKeepsLiveAppearance() {
        assertTrue(live("""
                {"animations":{"parallel":{"bones":{"prop":{"scale":{"0.0":0,"1.0":1}}}}}}
                """));
    }

    @Test
    void clipSpecificVisibilityKeepsLiveAppearance() {
        assertTrue(live("""
                {"animations":{"extra_hat":{"bones":{"hat":{"scale":0}}}}}
                """));
    }

    @Test
    void controllerSelectedVisibilityKeepsLiveAppearance() {
        assertTrue(DynamicModelAppearance.needsLiveRendering(List.of(json("""
                {"animations":{"parallel":{"bones":{"hat":{"scale":0}}}}}
                """)), List.of(json("""
                {"animation_controllers":{"wardrobe":{"states":{"default":{},"hat":{}}}}}
                """))));
    }

    @Test
    void nonIdentityAndConflictingScalesKeepLiveAppearance() {
        assertTrue(live("""
                {"animations":{"parallel":{"bones":{"body":{"scale":[1,2,1]}}}}}
                """));
        assertTrue(live("""
                {"animations":{
                  "parallel0":{"bones":{"hat":{"scale":0}}},
                  "parallel1":{"bones":{"hat":{"scale":1}}}
                }}
                """));
    }

    @Test
    void rotationOnlyModelsCanStillBeConverted() {
        assertFalse(live("""
                {"animations":{"idle":{"bones":{"tail":{"rotation":[0,"math.sin(q.anim_time)",0]}}}}}
                """));
    }
}
