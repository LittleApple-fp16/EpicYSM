package com.argorice.epicysm.client;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UniqueModelMatchTest {
    @Test
    void identicalKipfelSkeletonsAreAmbiguous() {
        assertNull(UniqueModelMatch.choose(Map.of("old", 1.0, "middle", 1.0, "new", 1.0), 0.75));
    }

    @Test
    void tiesDoNotDependOnScanOrder() {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("new", 1.0);
        scores.put("old", 1.0);
        assertNull(UniqueModelMatch.choose(scores, 0.75));
    }

    @Test
    void distinctSkeletonHasOneWinner() {
        assertEquals("new", UniqueModelMatch.choose(Map.of("old", 0.8, "new", 1.0), 0.75));
    }

    @Test
    void weakAndMissingMatchesAreRejected() {
        assertNull(UniqueModelMatch.choose(Map.of("old", 0.74), 0.75));
        assertNull(UniqueModelMatch.choose(Map.of(), 0.75));
    }

    @Test
    void tiedRunnersUpDoNotHideAUniqueWinner() {
        assertEquals("new", UniqueModelMatch.choose(Map.of("old", 0.8, "middle", 0.8, "new", 1.0), 0.75));
    }

    @Test
    void invalidScoresDoNotWin() {
        assertEquals("new", UniqueModelMatch.choose(Map.of("bad", Double.NaN, "new", 0.9), 0.75));
    }
}
