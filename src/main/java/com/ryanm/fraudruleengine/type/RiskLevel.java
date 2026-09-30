package com.ryanm.fraudruleengine.type;

public enum RiskLevel {
    LOW(0, 39),
    MEDIUM(40, 69),
    HIGH(70, 100);

    private final int minScore;
    private final int maxScore;

    RiskLevel(final int minScore, final int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static RiskLevel fromScore(final int score) {
        for (final RiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return HIGH;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }
}
