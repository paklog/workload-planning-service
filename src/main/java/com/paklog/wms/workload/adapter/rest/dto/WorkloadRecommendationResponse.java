package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;

import java.util.List;
import java.util.Map;

public record WorkloadRecommendationResponse(
    String warehouseId,
    Map<WorkloadCategory, CategoryRecommendation> categoryRecommendations,
    Map<ShiftType, ShiftRecommendation> shiftRecommendations,
    Double projectedUtilization,
    String balanceStatus,
    List<String> warnings,
    List<String> suggestions
) {
    public record CategoryRecommendation(
        WorkloadCategory category,
        Integer forecastedVolume,
        Integer requiredWorkers,
        Integer currentWorkers,
        Integer gap,
        Double requiredLaborHours
    ) {}

    public record ShiftRecommendation(
        ShiftType shift,
        Integer requiredWorkers,
        Integer currentWorkers,
        Integer gap,
        Double utilizationPercentage
    ) {}
}
