package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record GenerateForecastRequest(
    @NotBlank(message = "Warehouse ID is required")
    String warehouseId,

    @NotNull(message = "Forecast period is required")
    ForecastPeriod period,

    @NotNull(message = "Forecast date is required")
    LocalDateTime forecastDate,

    @NotNull(message = "Historical data is required")
    Map<WorkloadCategory, List<Integer>> historicalData
) {}
