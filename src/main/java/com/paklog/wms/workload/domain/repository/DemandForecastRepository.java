package com.paklog.wms.workload.domain.repository;

import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DemandForecast aggregate
 */
@Repository
public interface DemandForecastRepository extends MongoRepository<DemandForecast, String> {

    List<DemandForecast> findByWarehouseId(String warehouseId);

    List<DemandForecast> findByWarehouseIdAndPeriod(String warehouseId, ForecastPeriod period);

    Optional<DemandForecast> findByWarehouseIdAndPeriodAndForecastDate(
        String warehouseId, ForecastPeriod period, LocalDateTime forecastDate
    );

    List<DemandForecast> findByWarehouseIdAndForecastDateBetween(
        String warehouseId, LocalDateTime start, LocalDateTime end
    );

    List<DemandForecast> findByWarehouseIdOrderByForecastDateDesc(String warehouseId);
}
