package com.argorice.epicysm.compat;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YsmProviderTest {
    @Test
    void originalYsmSatisfiesTheDependency() {
        assertDoesNotThrow(() -> YsmProvider.require(Set.of("yes_steve_model")::contains));
    }

    @Test
    void openYsmAloneSatisfiesTheDependency() {
        assertDoesNotThrow(() -> YsmProvider.require(Set.of("openysm")::contains));
    }

    @Test
    void missingProvidersExplainBothSupportedChoices() {
        var error = assertThrows(IllegalStateException.class,
                () -> YsmProvider.require(Set.of("epicfight")::contains));
        assertTrue(error.getMessage().contains("yes_steve_model"));
        assertTrue(error.getMessage().contains("openysm"));
    }

    @Test
    void originalAndOpenYsmRuntimeClassesAreRecognized() {
        assertTrue(YsmProvider.isRuntimeClass("com.elfmcys.yesstevemodel.Bone"));
        assertTrue(YsmProvider.isRuntimeClass("org.openysm.geckolib3.geo.animated.AnimatedGeoBone"));
        assertTrue(YsmProvider.isRuntimeClass("org.openysm.event.api.SpecialPlayerRenderEvent"));
        assertTrue(YsmProvider.isRuntimeClass("rip.ysm.api.client.RenderLivingBridge"));
    }

    @Test
    void unrelatedClassesAndSimilarPackageNamesAreExcluded() {
        assertFalse(YsmProvider.isRuntimeClass("org.openysmhelper.Bone"));
        assertFalse(YsmProvider.isRuntimeClass("rip.ysmother.Renderer"));
        assertFalse(YsmProvider.isRuntimeClass("net.minecraft.client.renderer.ItemRenderer"));
        assertFalse(YsmProvider.isRuntimeClass((Class<?>) null));
    }

    @Test
    void eachProviderHasASelectionKeyAndModelDirectory() {
        for (String modId : YsmProvider.MOD_IDS) {
            assertTrue(YsmProvider.SELECTION_KEYS.contains(modId + ":model_id"));
            assertTrue(YsmProvider.MODEL_DIRECTORIES.contains(modId));
        }
    }

    @Test
    void diagnosticNamesStripOnlyTheRecognizedPackagePrefix() {
        assertEquals("yesstevemodel.Bone", YsmProvider.shortClassName("com.elfmcys.yesstevemodel.Bone"));
        assertEquals("geckolib3.geo.animated.AnimatedGeoBone",
                YsmProvider.shortClassName("org.openysm.geckolib3.geo.animated.AnimatedGeoBone"));
        assertEquals("other.Bone", YsmProvider.shortClassName("other.Bone"));
    }

    @Test
    void warningDetectionRecognizesEitherProvider() {
        assertTrue(YsmProvider.mentionedIn("openysm incompatible epicfight"));
        assertTrue(YsmProvider.mentionedIn("yes_steve_model incompatible epicfight"));
        assertFalse(YsmProvider.mentionedIn("unrelated incompatible epicfight"));
    }
}
