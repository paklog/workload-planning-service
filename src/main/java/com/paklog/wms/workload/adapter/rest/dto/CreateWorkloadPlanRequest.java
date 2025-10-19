package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record CreateWorkloadPlanRequest(
    @NotBlank(message = "Warehouse ID is required")
    String warehouseId,

    @NotNull(message = "Plan date is required")
    LocalDate planDate,

    @NotNull(message = "Planned volumes are required")
    Map<WorkloadCategory, Integer> plannedVolumes,

    String description
) {}
