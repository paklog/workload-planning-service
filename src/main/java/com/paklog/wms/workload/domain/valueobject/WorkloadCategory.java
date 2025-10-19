package com.paklog.wms.workload.domain.valueobject;

/**
 * Workload Category - Type of warehouse work
 */
public enum WorkloadCategory {
    RECEIVING("Receiving and putaway", 15.0),
    PICKING("Order picking", 25.0),
    PACKING("Packing and shipping", 20.0),
    REPLENISHMENT("Stock replenishment", 10.0),
    CYCLE_COUNTING("Cycle counting", 5.0),
    RETURNS("Returns processing", 8.0),
    VALUE_ADDED("Value-added services", 12.0),
    MAINTENANCE("Equipment maintenance", 5.0);

    private final String description;
    private final double standardProductivityRate; // Units per hour

    WorkloadCategory(String description, double standardProductivityRate) {
        this.description = description;
        this.standardProductivityRate = standardProductivityRate;
    }

    public String getDescription() {
        return description;
    }

    public double getStandardProductivityRate() {
        return standardProductivityRate;
    }

    /**
     * Calculate required labor hours for given volume
     */
    public double calculateLaborHours(int volume) {
        if (standardProductivityRate == 0) {
            return 0;
        }
        return volume / standardProductivityRate;
    }

    /**
     * Calculate required workers for given volume and shift hours
     */
    public int calculateRequiredWorkers(int volume, int shiftHours) {
        double laborHours = calculateLaborHours(volume);
        return (int) Math.ceil(laborHours / shiftHours);
    }

    /**
     * Check if this is a core warehouse operation
     */
    public boolean isCoreOperation() {
        return switch (this) {
            case RECEIVING, PICKING, PACKING, REPLENISHMENT -> true;
            case CYCLE_COUNTING, RETURNS, VALUE_ADDED, MAINTENANCE -> false;
        };
    }
}
