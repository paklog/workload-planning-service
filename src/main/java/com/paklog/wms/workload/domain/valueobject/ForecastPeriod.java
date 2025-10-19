package com.paklog.wms.workload.domain.valueobject;

/**
 * Forecast Period - Time horizon for demand forecasting
 */
public enum ForecastPeriod {
    HOURLY("Hourly forecast", 1, 24),
    DAILY("Daily forecast", 24, 30),
    WEEKLY("Weekly forecast", 168, 12),
    MONTHLY("Monthly forecast", 720, 6);

    private final String description;
    private final int hoursPerPeriod;
    private final int periodsAhead;

    ForecastPeriod(String description, int hoursPerPeriod, int periodsAhead) {
        this.description = description;
        this.hoursPerPeriod = hoursPerPeriod;
        this.periodsAhead = periodsAhead;
    }

    public String getDescription() {
        return description;
    }

    public int getHoursPerPeriod() {
        return hoursPerPeriod;
    }

    public int getPeriodsAhead() {
        return periodsAhead;
    }

    /**
     * Get total forecast horizon in hours
     */
    public int getTotalForecastHours() {
        return hoursPerPeriod * periodsAhead;
    }

    /**
     * Check if this is short-term forecasting
     */
    public boolean isShortTerm() {
        return this == HOURLY || this == DAILY;
    }

    /**
     * Check if this is long-term forecasting
     */
    public boolean isLongTerm() {
        return this == WEEKLY || this == MONTHLY;
    }
}
