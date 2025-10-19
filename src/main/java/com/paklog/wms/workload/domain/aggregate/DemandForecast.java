package com.paklog.wms.workload.domain.aggregate;

import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DemandForecast - Aggregate root for demand forecasting
 *
 * Uses time series analysis to predict future workload volumes
 */
@Document(collection = "demand_forecasts")
public class DemandForecast {

    @Id
    private String forecastId;

    @Indexed
    private String warehouseId;

    private ForecastPeriod period;

    @Indexed
    private LocalDateTime forecastDate;

    private LocalDateTime createdAt;

    // Forecast data points (time -> category -> volume)
    private List<ForecastDataPoint> dataPoints;

    // Historical accuracy metrics
    private Double accuracy; // Percentage
    private Double meanAbsoluteError;
    private Double meanSquaredError;

    // Model metadata
    private String forecastingModel; // e.g., "MOVING_AVERAGE", "EXPONENTIAL_SMOOTHING", "ARIMA"
    private Map<String, Object> modelParameters;

    protected DemandForecast() {
        this.dataPoints = new ArrayList<>();
        this.modelParameters = new HashMap<>();
    }

    /**
     * Create a new demand forecast
     */
    public static DemandForecast create(
            String forecastId,
            String warehouseId,
            ForecastPeriod period,
            LocalDateTime forecastDate
    ) {
        DemandForecast forecast = new DemandForecast();
        forecast.forecastId = forecastId;
        forecast.warehouseId = warehouseId;
        forecast.period = period;
        forecast.forecastDate = forecastDate;
        forecast.createdAt = LocalDateTime.now();
        forecast.dataPoints = new ArrayList<>();
        forecast.modelParameters = new HashMap<>();

        return forecast;
    }

    /**
     * Add forecast data point
     */
    public void addDataPoint(LocalDateTime timestamp, WorkloadCategory category,
                            int forecastedVolume, Double confidenceInterval) {
        ForecastDataPoint point = new ForecastDataPoint(
            timestamp, category, forecastedVolume, confidenceInterval
        );
        dataPoints.add(point);
    }

    /**
     * Set forecasting model
     */
    public void setForecastingModel(String model, Map<String, Object> parameters) {
        this.forecastingModel = model;
        this.modelParameters = parameters != null ? parameters : new HashMap<>();
    }

    /**
     * Update accuracy metrics
     */
    public void updateAccuracyMetrics(double accuracy, double mae, double mse) {
        this.accuracy = accuracy;
        this.meanAbsoluteError = mae;
        this.meanSquaredError = mse;
    }

    /**
     * Get forecasted volume for specific time and category
     */
    public int getForecastedVolume(LocalDateTime timestamp, WorkloadCategory category) {
        return dataPoints.stream()
            .filter(dp -> dp.timestamp().equals(timestamp) && dp.category() == category)
            .findFirst()
            .map(ForecastDataPoint::forecastedVolume)
            .orElse(0);
    }

    /**
     * Get total forecasted volume for category
     */
    public int getTotalForecastedVolume(WorkloadCategory category) {
        return dataPoints.stream()
            .filter(dp -> dp.category() == category)
            .mapToInt(ForecastDataPoint::forecastedVolume)
            .sum();
    }

    /**
     * Get peak demand period
     */
    public LocalDateTime getPeakDemandTime(WorkloadCategory category) {
        return dataPoints.stream()
            .filter(dp -> dp.category() == category)
            .max((dp1, dp2) -> Integer.compare(dp1.forecastedVolume(), dp2.forecastedVolume()))
            .map(ForecastDataPoint::timestamp)
            .orElse(null);
    }

    /**
     * Check if forecast is accurate (>85%)
     */
    public boolean isAccurate() {
        return accuracy != null && accuracy >= 85.0;
    }

    /**
     * Check if forecast needs refresh
     */
    public boolean needsRefresh() {
        if (createdAt == null) {
            return true;
        }

        // Refresh based on period
        long hoursOld = java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
        return switch (period) {
            case HOURLY -> hoursOld >= 1;
            case DAILY -> hoursOld >= 24;
            case WEEKLY -> hoursOld >= 168;
            case MONTHLY -> hoursOld >= 720;
        };
    }

    // Getters
    public String getForecastId() {
        return forecastId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public ForecastPeriod getPeriod() {
        return period;
    }

    public LocalDateTime getForecastDate() {
        return forecastDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ForecastDataPoint> getDataPoints() {
        return new ArrayList<>(dataPoints);
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public Double getMeanAbsoluteError() {
        return meanAbsoluteError;
    }

    public Double getMeanSquaredError() {
        return meanSquaredError;
    }

    public String getForecastingModel() {
        return forecastingModel;
    }

    public Map<String, Object> getModelParameters() {
        return new HashMap<>(modelParameters);
    }

    /**
     * Forecast data point
     */
    public record ForecastDataPoint(
        LocalDateTime timestamp,
        WorkloadCategory category,
        int forecastedVolume,
        Double confidenceInterval
    ) {}

    @Override
    public String toString() {
        return String.format("DemandForecast[id=%s, warehouse=%s, period=%s, dataPoints=%d, accuracy=%s%%]",
            forecastId, warehouseId, period, dataPoints.size(), accuracy);
    }
}
