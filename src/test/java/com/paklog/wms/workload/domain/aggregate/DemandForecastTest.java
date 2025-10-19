package com.paklog.wms.workload.domain.aggregate;

import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemandForecastTest {

    @Test
    void shouldCalculateTotalsAndPeaks() {
        LocalDateTime baseTime = LocalDateTime.now();
        DemandForecast forecast = DemandForecast.create(
            "forecast-1",
            "WH-1",
            ForecastPeriod.DAILY,
            baseTime
        );

        forecast.setForecastingModel("MOVING_AVERAGE", Map.of("window", 7));

        forecast.addDataPoint(baseTime, WorkloadCategory.PICKING, 120, 5.0);
        forecast.addDataPoint(baseTime.plusHours(24), WorkloadCategory.PICKING, 180, 7.5);
        forecast.addDataPoint(baseTime.plusHours(48), WorkloadCategory.RECEIVING, 90, 4.0);

        assertThat(forecast.getForecastingModel()).isEqualTo("MOVING_AVERAGE");
        assertThat(forecast.getModelParameters()).containsEntry("window", 7);

        assertThat(forecast.getDataPoints())
            .hasSize(3)
            .extracting(DemandForecast.ForecastDataPoint::confidenceInterval)
            .contains(5.0, 7.5, 4.0);

        assertThat(forecast.getTotalForecastedVolume(WorkloadCategory.PICKING)).isEqualTo(300);
        assertThat(forecast.getTotalForecastedVolume(WorkloadCategory.RECEIVING)).isEqualTo(90);
        assertThat(forecast.getForecastedVolume(baseTime.plusHours(24), WorkloadCategory.PICKING))
            .isEqualTo(180);
        assertThat(forecast.getPeakDemandTime(WorkloadCategory.PICKING))
            .isEqualTo(baseTime.plusHours(24));
    }

    @Test
    void shouldEvaluateAccuracyAndRefreshNeed() {
        LocalDateTime baseTime = LocalDateTime.now();
        DemandForecast forecast = DemandForecast.create(
            "forecast-2",
            "WH-2",
            ForecastPeriod.HOURLY,
            baseTime
        );

        forecast.updateAccuracyMetrics(92.5, 4.0, 16.0);
        assertThat(forecast.isAccurate()).isTrue();
        assertThat(forecast.getAccuracy()).isEqualTo(92.5);
        assertThat(forecast.getMeanAbsoluteError()).isEqualTo(4.0);
        assertThat(forecast.getMeanSquaredError()).isEqualTo(16.0);

        assertThat(forecast.needsRefresh()).isFalse();

        ReflectionTestUtils.setField(forecast, "createdAt", baseTime.minusHours(3));

        assertThat(forecast.needsRefresh()).isTrue();
    }
}
