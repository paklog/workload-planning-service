package com.paklog.wms.workload.domain.valueobject;

/**
 * Skill Level - Worker skill and experience level
 */
public enum SkillLevel {
    TRAINEE("Trainee", 0.6),
    JUNIOR("Junior", 0.8),
    INTERMEDIATE("Intermediate", 1.0),
    SENIOR("Senior", 1.2),
    EXPERT("Expert", 1.4),
    LEAD("Team Lead", 1.3);

    private final String description;
    private final double productivityMultiplier;

    SkillLevel(String description, double productivityMultiplier) {
        this.description = description;
        this.productivityMultiplier = productivityMultiplier;
    }

    public String getDescription() {
        return description;
    }

    public double getProductivityMultiplier() {
        return productivityMultiplier;
    }

    /**
     * Calculate effective productivity rate
     */
    public double calculateEffectiveRate(double standardRate) {
        return standardRate * productivityMultiplier;
    }

    /**
     * Check if worker can train others
     */
    public boolean canTrainOthers() {
        return this == SENIOR || this == EXPERT || this == LEAD;
    }

    /**
     * Check if worker can lead a team
     */
    public boolean canLeadTeam() {
        return this == LEAD || this == EXPERT;
    }
}
