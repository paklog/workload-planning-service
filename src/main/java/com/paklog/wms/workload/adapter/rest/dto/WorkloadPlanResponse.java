package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record WorkloadPlanResponse(
    String planId,
    String warehouseId,
    LocalDate planDate,
    Map<WorkloadCategory, Integer> plannedVolumes,
    Map<ShiftType, List<ShiftAssignmentDto>> shiftAssignments,
    Integer totalRequiredLaborHours,
    Integer totalAvailableLaborHours,
    Double utilizationPercentage,
    String status,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static WorkloadPlanResponse from(WorkloadPlan plan) {
        return new WorkloadPlanResponse(
            plan.getPlanId(),
            plan.getWarehouseId(),
            plan.getPlanDate(),
            plan.getPlannedVolumes(),
            plan.getShiftAssignments().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream().map(ShiftAssignmentDto::from).toList()
                )),
            plan.getTotalRequiredLaborHours(),
            plan.getTotalAvailableLaborHours(),
            plan.getUtilizationPercentage(),
            plan.getStatus().name(),
            plan.getNotes(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }

    public record ShiftAssignmentDto(
        String workerId,
        String workerName,
        WorkloadCategory primaryCategory,
        Integer plannedHours
    ) {
        public static ShiftAssignmentDto from(WorkloadPlan.ShiftAssignment assignment) {
            return new ShiftAssignmentDto(
                assignment.workerId(),
                assignment.workerName(),
                assignment.primaryCategory(),
                assignment.plannedHours()
            );
        }
    }
}
