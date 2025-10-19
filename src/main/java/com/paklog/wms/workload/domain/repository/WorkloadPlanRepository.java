package com.paklog.wms.workload.domain.repository;

import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkloadPlan aggregate
 */
@Repository
public interface WorkloadPlanRepository extends MongoRepository<WorkloadPlan, String> {

    List<WorkloadPlan> findByWarehouseId(String warehouseId);

    Optional<WorkloadPlan> findByWarehouseIdAndPlanDate(String warehouseId, LocalDate planDate);

    List<WorkloadPlan> findByWarehouseIdAndPlanDateBetween(
        String warehouseId, LocalDate start, LocalDate end
    );

    List<WorkloadPlan> findByWarehouseIdAndStatus(
        String warehouseId, WorkloadPlan.PlanStatus status
    );

    List<WorkloadPlan> findByWarehouseIdOrderByPlanDateDesc(String warehouseId);
}
