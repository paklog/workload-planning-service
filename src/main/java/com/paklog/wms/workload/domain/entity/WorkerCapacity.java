package com.paklog.wms.workload.domain.entity;

import com.paklog.wms.workload.domain.valueobject.SkillLevel;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;

import java.util.HashMap;
import java.util.Map;

/**
 * Worker Capacity - Individual worker's capacity and skills
 */
public class WorkerCapacity {
    private String workerId;
    private String name;
    private SkillLevel skillLevel;
    private Map<WorkloadCategory, Double> productivityRates;
    private Integer maxHoursPerWeek;
    private Boolean isFullTime;
    private Double hourlyRate;

    protected WorkerCapacity() {
        this.productivityRates = new HashMap<>();
    }

    public WorkerCapacity(String workerId, String name, SkillLevel skillLevel,
                         Integer maxHoursPerWeek, Boolean isFullTime, Double hourlyRate) {
        this.workerId = workerId;
        this.name = name;
        this.skillLevel = skillLevel;
        this.productivityRates = new HashMap<>();
        this.maxHoursPerWeek = maxHoursPerWeek;
        this.isFullTime = isFullTime;
        this.hourlyRate = hourlyRate;
    }

    /**
     * Set productivity rate for a category
     */
    public void setProductivityRate(WorkloadCategory category, double rate) {
        this.productivityRates.put(category, rate);
    }

    /**
     * Get effective productivity rate for a category
     */
    public double getEffectiveProductivityRate(WorkloadCategory category) {
        double baseRate = productivityRates.getOrDefault(category,
            category.getStandardProductivityRate());
        return skillLevel.calculateEffectiveRate(baseRate);
    }

    /**
     * Calculate output for given category and hours
     */
    public double calculateOutput(WorkloadCategory category, double hours) {
        return getEffectiveProductivityRate(category) * hours;
    }

    /**
     * Calculate labor cost for given hours and shift
     */
    public double calculateLaborCost(double hours, double shiftPremiumMultiplier) {
        return hours * hourlyRate * shiftPremiumMultiplier;
    }

    /**
     * Check if worker can perform category
     */
    public boolean canPerform(WorkloadCategory category) {
        return productivityRates.containsKey(category) ||
               (category.isCoreOperation() && skillLevel != SkillLevel.TRAINEE);
    }

    // Getters
    public String getWorkerId() {
        return workerId;
    }

    public String getName() {
        return name;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public Map<WorkloadCategory, Double> getProductivityRates() {
        return new HashMap<>(productivityRates);
    }

    public Integer getMaxHoursPerWeek() {
        return maxHoursPerWeek;
    }

    public Boolean getIsFullTime() {
        return isFullTime;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    @Override
    public String toString() {
        return String.format("WorkerCapacity[id=%s, name=%s, skill=%s, fullTime=%s]",
            workerId, name, skillLevel, isFullTime);
    }
}
