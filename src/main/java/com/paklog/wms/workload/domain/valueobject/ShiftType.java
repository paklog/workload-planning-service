package com.paklog.wms.workload.domain.valueobject;

import java.time.LocalTime;

/**
 * Shift Type - Defines warehouse operating shifts
 */
public enum ShiftType {
    DAY_SHIFT("Day Shift", LocalTime.of(6, 0), LocalTime.of(14, 0), 8),
    EVENING_SHIFT("Evening Shift", LocalTime.of(14, 0), LocalTime.of(22, 0), 8),
    NIGHT_SHIFT("Night Shift", LocalTime.of(22, 0), LocalTime.of(6, 0), 8),
    MORNING_SHIFT("Morning Shift", LocalTime.of(8, 0), LocalTime.of(17, 0), 8),
    AFTERNOON_SHIFT("Afternoon Shift", LocalTime.of(12, 0), LocalTime.of(21, 0), 8),
    OVERNIGHT_SHIFT("Overnight Shift", LocalTime.of(0, 0), LocalTime.of(8, 0), 8),
    WEEKEND_DAY("Weekend Day", LocalTime.of(7, 0), LocalTime.of(15, 0), 8),
    WEEKEND_NIGHT("Weekend Night", LocalTime.of(15, 0), LocalTime.of(23, 0), 8);

    private final String description;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int durationHours;

    ShiftType(String description, LocalTime startTime, LocalTime endTime, int durationHours) {
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
    }

    public String getDescription() {
        return description;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    /**
     * Check if this is a weekend shift
     */
    public boolean isWeekendShift() {
        return this == WEEKEND_DAY || this == WEEKEND_NIGHT;
    }

    /**
     * Check if this is a night shift
     */
    public boolean isNightShift() {
        return this == NIGHT_SHIFT || this == OVERNIGHT_SHIFT || this == WEEKEND_NIGHT;
    }

    /**
     * Get premium multiplier for labor cost
     */
    public double getPremiumMultiplier() {
        if (isNightShift()) {
            return 1.25; // 25% night premium
        }
        if (isWeekendShift()) {
            return 1.50; // 50% weekend premium
        }
        return 1.0; // Standard rate
    }
}
