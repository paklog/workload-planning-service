package com.paklog.wms.workload.adapter.rest.dto;

import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;

import java.time.LocalDateTime;
import java.util.List;

public record ForecastResponse(
    String forecastId,
    String warehouseId,
    ForecastPeriod period,
    LocalDateTime forecastDate,
    String forecastingModel,
    Double accuracy,
    Double meanAbsoluteError,
    List<ForecastDataPointDto> dataPoints,
    LocalDateTime createdAt
) {
    public static ForecastResponse from(DemandForecast forecast) {
        return new ForecastResponse(
            forecast.getForecastId(),
            forecast.getWarehouseId(),
            forecast.getPeriod(),
            forecast.getForecastDate(),
            forecast.getForecastingModel(),
            forecast.getAccuracy(),
            forecast.getMeanAbsoluteError(),
            forecast.getDataPoints().stream().map(ForecastDataPointDto::from).toList(),
            forecast.getCreatedAt()
        );
    }

    public record ForecastDataPointDto(
        LocalDateTime timestamp,
        WorkloadCategory category,
        Integer forecastedVolume,
        Double confidenceInterval
    ) {
        public static ForecastDataPointDto from(DemandForecast.ForecastDataPoint point) {
            return new ForecastDataPointDto(
                point.timestamp(),
                point.category(),
                point.forecastedVolume(),
                point.confidenceInterval()
            );
        }
    }
}
