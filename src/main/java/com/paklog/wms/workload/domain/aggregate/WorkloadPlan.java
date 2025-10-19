package com.paklog.wms.workload.domain.aggregate;

import com.paklog.wms.workload.domain.entity.WorkerCapacity;
import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkloadPlan - Aggregate root for workload and labor planning
 *
 * Combines demand forecasts with labor capacity to create optimal staffing plans
 */
@Document(collection = "workload_plans")
public class WorkloadPlan {

    @Id
    private String planId;

    @Indexed
    private String warehouseId;

    @Indexed
    private LocalDate planDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Planned workload by category
    private Map<WorkloadCategory, Integer> plannedVolumes;

    // Shift assignments (shift -> list of assigned workers)
    private Map<ShiftType, List<ShiftAssignment>> shiftAssignments;

    // Capacity metrics
    private Integer totalRequiredLaborHours;
    private Integer totalAvailableLaborHours;
    private Double utilizationPercentage;
    private Double estimatedLaborCost;

    // Status
    private PlanStatus status;
    private String notes;

    protected WorkloadPlan() {
        this.plannedVolumes = new HashMap<>();
        this.shiftAssignments = new HashMap<>();
    }

    /**
     * Create a new workload plan
     */
    public static WorkloadPlan create(String planId, String warehouseId, LocalDate planDate) {
        WorkloadPlan plan = new WorkloadPlan();
        plan.planId = planId;
        plan.warehouseId = warehouseId;
        plan.planDate = planDate;
        plan.createdAt = LocalDateTime.now();
        plan.updatedAt = LocalDateTime.now();
        plan.plannedVolumes = new HashMap<>();
        plan.shiftAssignments = new HashMap<>();
        plan.status = PlanStatus.DRAFT;
        plan.totalRequiredLaborHours = 0;
        plan.totalAvailableLaborHours = 0;
        plan.utilizationPercentage = 0.0;
        plan.estimatedLaborCost = 0.0;

        return plan;
    }

    /**
     * Set planned volume for a category
     */
    public void setPlannedVolume(WorkloadCategory category, int volume) {
        plannedVolumes.put(category, volume);
        recalculateMetrics();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Assign worker to shift
     */
    public void assignWorkerToShift(ShiftType shift, String workerId, String workerName,
                                    WorkloadCategory primaryCategory, int plannedHours) {
        ShiftAssignment assignment = new ShiftAssignment(
            workerId, workerName, primaryCategory, plannedHours
        );

        shiftAssignments.computeIfAbsent(shift, k -> new ArrayList<>()).add(assignment);
        recalculateMetrics();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remove worker from shift
     */
    public void removeWorkerFromShift(ShiftType shift, String workerId) {
        List<ShiftAssignment> assignments = shiftAssignments.get(shift);
        if (assignments != null) {
            assignments.removeIf(a -> a.workerId().equals(workerId));
            recalculateMetrics();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Approve the plan
     */
    public void approve() {
        if (status == PlanStatus.DRAFT) {
            this.status = PlanStatus.APPROVED;
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Only draft plans can be approved");
        }
    }

    /**
     * Publish the plan
     */
    public void publish() {
        if (status == PlanStatus.APPROVED) {
            this.status = PlanStatus.PUBLISHED;
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Only approved plans can be published");
        }
    }

    /**
     * Cancel the plan
     */
    public void cancel(String reason) {
        this.status = PlanStatus.CANCELLED;
        this.notes = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get workers assigned to a shift
     */
    public List<ShiftAssignment> getShiftAssignments(ShiftType shift) {
        return new ArrayList<>(shiftAssignments.getOrDefault(shift, new ArrayList<>()));
    }

    /**
     * Get total workers assigned
     */
    public int getTotalWorkersAssigned() {
        return shiftAssignments.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Get total hours assigned for a shift
     */
    public int getTotalHoursForShift(ShiftType shift) {
        return shiftAssignments.getOrDefault(shift, new ArrayList<>()).stream()
            .mapToInt(ShiftAssignment::plannedHours)
            .sum();
    }

    /**
     * Calculate required labor hours for all categories
     */
    public int calculateRequiredLaborHours() {
        return plannedVolumes.entrySet().stream()
            .mapToInt(entry -> {
                WorkloadCategory category = entry.getKey();
                int volume = entry.getValue();
                return (int) Math.ceil(category.calculateLaborHours(volume));
            })
            .sum();
    }

    /**
     * Check if plan is understaffed
     */
    public boolean isUnderstaffed() {
        return utilizationPercentage != null && utilizationPercentage < 85.0;
    }

    /**
     * Check if plan is overstaffed
     */
    public boolean isOverstaffed() {
        return utilizationPercentage != null && utilizationPercentage > 110.0;
    }

    /**
     * Check if plan is balanced
     */
    public boolean isBalanced() {
        return utilizationPercentage != null &&
               utilizationPercentage >= 85.0 &&
               utilizationPercentage <= 110.0;
    }

    /**
     * Recalculate all metrics
     */
    private void recalculateMetrics() {
        // Calculate required hours
        this.totalRequiredLaborHours = calculateRequiredLaborHours();

        // Calculate available hours
        this.totalAvailableLaborHours = shiftAssignments.values().stream()
            .flatMap(List::stream)
            .mapToInt(ShiftAssignment::plannedHours)
            .sum();

        // Calculate utilization
        if (totalAvailableLaborHours > 0) {
            this.utilizationPercentage = (totalRequiredLaborHours * 100.0) / totalAvailableLaborHours;
        } else {
            this.utilizationPercentage = 0.0;
        }

        // Calculate estimated cost (simplified)
        this.estimatedLaborCost = totalAvailableLaborHours * 25.0; // Avg $25/hour
    }

    // Getters
    public String getPlanId() {
        return planId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Map<WorkloadCategory, Integer> getPlannedVolumes() {
        return new HashMap<>(plannedVolumes);
    }

    public Map<ShiftType, List<ShiftAssignment>> getShiftAssignments() {
        return new HashMap<>(shiftAssignments);
    }

    public Integer getTotalRequiredLaborHours() {
        return totalRequiredLaborHours;
    }

    public Integer getTotalAvailableLaborHours() {
        return totalAvailableLaborHours;
    }

    public Double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public Double getEstimatedLaborCost() {
        return estimatedLaborCost;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * Shift assignment record
     */
    public record ShiftAssignment(
        String workerId,
        String workerName,
        WorkloadCategory primaryCategory,
        int plannedHours
    ) {}

    /**
     * Plan status
     */
    public enum PlanStatus {
        DRAFT,
        APPROVED,
        PUBLISHED,
        CANCELLED
    }

    @Override
    public String toString() {
        return String.format("WorkloadPlan[id=%s, date=%s, status=%s, workers=%d, util=%s%%]",
            planId, planDate, status, getTotalWorkersAssigned(), utilizationPercentage);
    }
}
