package com.argorice.epicysm.client;

import java.util.Map;

/** Selects a model only when its skeleton score is uniquely best. */
final class UniqueModelMatch {
    private UniqueModelMatch() {
    }

    static String choose(Map<String, Double> scores, double minimum) {
        String best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double secondScore = Double.NEGATIVE_INFINITY;

        for (var entry : scores.entrySet()) {
            double score = entry.getValue();
            if (!Double.isFinite(score)) {
                continue;
            }

            if (score > bestScore) {
                secondScore = bestScore;
                bestScore = score;
                best = entry.getKey();
            } else if (score > secondScore) {
                secondScore = score;
            }
        }

        return bestScore >= minimum && bestScore - secondScore > 1.0E-9 ? best : null;
    }
}
