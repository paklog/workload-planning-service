package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignWorkerRequest(
    @NotNull(message = "Shift type is required")
    ShiftType shiftType,

    @NotBlank(message = "Worker ID is required")
    String workerId,

    @NotBlank(message = "Worker name is required")
    String workerName,

    @NotNull(message = "Primary category is required")
    WorkloadCategory primaryCategory,

    @NotNull(message = "Planned hours are required")
    @Positive(message = "Planned hours must be positive")
    Integer plannedHours
) {}
