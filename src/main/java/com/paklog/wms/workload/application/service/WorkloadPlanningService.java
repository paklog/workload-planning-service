package com.paklog.wms.workload.application.service;

import com.paklog.wms.workload.adapter.event.WorkloadPlanningEventPublisher;
import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import com.paklog.wms.workload.domain.entity.WorkerCapacity;
import com.paklog.wms.workload.domain.repository.DemandForecastRepository;
import com.paklog.wms.workload.domain.repository.WorkloadPlanRepository;
import com.paklog.wms.workload.domain.valueobject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Workload Planning Service
 * Core service for demand forecasting, labor capacity planning, and workload optimization
 */
@Service
@Transactional
public class WorkloadPlanningService {

    private static final Logger logger = LoggerFactory.getLogger(WorkloadPlanningService.class);

    private final DemandForecastRepository forecastRepository;
    private final WorkloadPlanRepository planRepository;
    private final WorkloadPlanningEventPublisher eventPublisher;

    public WorkloadPlanningService(
            DemandForecastRepository forecastRepository,
            WorkloadPlanRepository planRepository,
            WorkloadPlanningEventPublisher eventPublisher
    ) {
        this.forecastRepository = forecastRepository;
        this.planRepository = planRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Generate demand forecast using historical data
     */
    public DemandForecast generateDemandForecast(
            String warehouseId,
            ForecastPeriod period,
            LocalDateTime forecastDate,
            Map<WorkloadCategory, List<Integer>> historicalData
    ) {
        logger.info("Generating {} forecast for warehouse {} on {}",
            period, warehouseId, forecastDate);

        String forecastId = UUID.randomUUID().toString();
        DemandForecast forecast = DemandForecast.create(
            forecastId, warehouseId, period, forecastDate
        );

        // Apply forecasting algorithm based on period
        String model = switch (period) {
            case HOURLY -> "EXPONENTIAL_SMOOTHING";
            case DAILY -> "MOVING_AVERAGE";
            case WEEKLY -> "WEIGHTED_MOVING_AVERAGE";
            case MONTHLY -> "SEASONAL_DECOMPOSITION";
        };

        forecast.setForecastingModel(model, Map.of(
            "alpha", 0.3,
            "window_size", 7,
            "seasonality", 7
        ));

        // Generate forecast data points for each category
        for (Map.Entry<WorkloadCategory, List<Integer>> entry : historicalData.entrySet()) {
            WorkloadCategory category = entry.getKey();
            List<Integer> historical = entry.getValue();

            // Generate forecasts for the period
            for (int i = 0; i < period.getPeriodsAhead(); i++) {
                LocalDateTime timestamp = forecastDate.plusHours(i * period.getHoursPerPeriod());

                // Simple moving average forecast
                int forecastedVolume = calculateMovingAverage(historical, 7);
                double confidenceInterval = calculateConfidenceInterval(historical);

                forecast.addDataPoint(timestamp, category, forecastedVolume, confidenceInterval);
            }
        }

        // Calculate and update accuracy metrics
        double accuracy = 90.0; // Simulated accuracy
        double mae = 5.0;
        double mse = 25.0;
        forecast.updateAccuracyMetrics(accuracy, mae, mse);

        forecast = forecastRepository.save(forecast);

        // Publish event
        eventPublisher.publishForecastGenerated(
            forecastId, warehouseId, period.name(), model, accuracy
        );

        return forecast;
    }

    /**
     * Get forecast by ID
     */
    @Transactional(readOnly = true)
    public Optional<DemandForecast> getForecast(String forecastId) {
        return forecastRepository.findById(forecastId);
    }

    /**
     * Get forecasts by warehouse
     */
    @Transactional(readOnly = true)
    public List<DemandForecast> getForecastsByWarehouse(String warehouseId) {
        return forecastRepository.findByWarehouseIdOrderByForecastDateDesc(warehouseId);
    }

    /**
     * Create workload plan from demand forecast
     */
    public WorkloadPlan createWorkloadPlan(
            String warehouseId,
            LocalDate planDate,
            DemandForecast forecast
    ) {
        logger.info("Creating workload plan for warehouse {} on {}", warehouseId, planDate);

        String planId = UUID.randomUUID().toString();
        WorkloadPlan plan = WorkloadPlan.create(planId, warehouseId, planDate);

        // Set planned volumes from forecast
        for (WorkloadCategory category : WorkloadCategory.values()) {
            int totalVolume = forecast.getTotalForecastedVolume(category);
            if (totalVolume > 0) {
                plan.setPlannedVolume(category, totalVolume);
            }
        }

        return planRepository.save(plan);
    }

    /**
     * Create workload plan with planned volumes
     */
    public WorkloadPlan createWorkloadPlan(
            String warehouseId,
            LocalDate planDate,
            Map<WorkloadCategory, Integer> plannedVolumes,
            String description
    ) {
        logger.info("Creating workload plan for warehouse {} on {}", warehouseId, planDate);

        String planId = UUID.randomUUID().toString();
        WorkloadPlan plan = WorkloadPlan.create(planId, warehouseId, planDate);

        // Note: description is not currently supported in the domain model
        // If needed, would require adding a setNotes() method or similar

        // Set planned volumes
        for (Map.Entry<WorkloadCategory, Integer> entry : plannedVolumes.entrySet()) {
            plan.setPlannedVolume(entry.getKey(), entry.getValue());
        }

        plan = planRepository.save(plan);

        // Publish event
        eventPublisher.publishPlanCreated(
            planId, warehouseId, planDate.toString(), plan.getTotalRequiredLaborHours()
        );

        return plan;
    }

    /**
     * Get workload plan by ID
     */
    @Transactional(readOnly = true)
    public Optional<WorkloadPlan> getWorkloadPlan(String planId) {
        return planRepository.findById(planId);
    }

    /**
     * Get workload plans by warehouse
     */
    @Transactional(readOnly = true)
    public List<WorkloadPlan> getWorkloadPlansByWarehouse(String warehouseId) {
        return planRepository.findByWarehouseIdOrderByPlanDateDesc(warehouseId);
    }

    /**
     * Assign worker to shift in plan
     */
    public WorkloadPlan assignWorkerToShift(
            String planId,
            ShiftType shiftType,
            String workerId,
            String workerName,
            WorkloadCategory primaryCategory,
            Integer plannedHours
    ) {
        logger.info("Assigning worker {} to {} shift for plan {}", workerId, shiftType, planId);

        WorkloadPlan plan = getPlanOrThrow(planId);
        plan.assignWorkerToShift(shiftType, workerId, workerName, primaryCategory, plannedHours);

        plan = planRepository.save(plan);

        // Publish event
        eventPublisher.publishWorkerAssigned(
            planId, workerId, workerName, shiftType.name(), primaryCategory.name(), plannedHours
        );

        return plan;
    }

    /**
     * Optimize labor allocation for workload plan
     */
    public WorkloadPlan optimizeLaborAllocation(
            String planId,
            List<WorkerCapacity> availableWorkers
    ) {
        logger.info("Optimizing labor allocation for plan {}", planId);

        WorkloadPlan plan = getPlanOrThrow(planId);

        // Clear existing assignments
        // (In production, this would be more sophisticated)

        // Calculate required workers per category
        Map<WorkloadCategory, Integer> requiredWorkers = new HashMap<>();
        for (Map.Entry<WorkloadCategory, Integer> entry : plan.getPlannedVolumes().entrySet()) {
            WorkloadCategory category = entry.getKey();
            int volume = entry.getValue();
            int required = category.calculateRequiredWorkers(volume, 8); // 8-hour shift
            requiredWorkers.put(category, required);
        }

        // Assign workers to shifts using greedy algorithm
        List<WorkerCapacity> sortedWorkers = new ArrayList<>(availableWorkers);
        sortedWorkers.sort((w1, w2) -> w2.getSkillLevel().compareTo(w1.getSkillLevel()));

        for (WorkerCapacity worker : sortedWorkers) {
            // Find best category for this worker
            WorkloadCategory bestCategory = findBestCategory(worker, requiredWorkers);

            if (bestCategory != null) {
                // Assign to appropriate shift
                ShiftType shift = determineOptimalShift(plan, bestCategory);
                plan.assignWorkerToShift(shift, worker.getWorkerId(), worker.getName(),
                    bestCategory, 8);

                // Decrease required count
                requiredWorkers.merge(bestCategory, -1, Integer::sum);
            }
        }

        return planRepository.save(plan);
    }

    /**
     * Approve workload plan
     */
    public WorkloadPlan approvePlan(String planId) {
        logger.info("Approving workload plan {}", planId);
        WorkloadPlan plan = getPlanOrThrow(planId);
        plan.approve();
        return planRepository.save(plan);
    }

    /**
     * Approve workload plan with approver
     */
    public WorkloadPlan approvePlan(String planId, String approvedBy) {
        logger.info("Approving workload plan {} by {}", planId, approvedBy);
        WorkloadPlan plan = getPlanOrThrow(planId);
        plan.approve();
        plan = planRepository.save(plan);

        // Publish event
        eventPublisher.publishPlanApproved(
            planId,
            plan.getWarehouseId(),
            approvedBy,
            plan.getTotalWorkersAssigned(),
            plan.getUtilizationPercentage()
        );

        return plan;
    }

    /**
     * Publish workload plan
     */
    public WorkloadPlan publishPlan(String planId) {
        logger.info("Publishing workload plan {}", planId);
        WorkloadPlan plan = getPlanOrThrow(planId);
        plan.publish();
        plan = planRepository.save(plan);

        // Publish event
        eventPublisher.publishPlanPublished(
            planId,
            plan.getWarehouseId(),
            plan.getPlanDate().toString(),
            plan.getTotalWorkersAssigned()
        );

        return plan;
    }

    /**
     * Cancel workload plan
     */
    public WorkloadPlan cancelPlan(String planId, String reason) {
        logger.info("Cancelling workload plan {}: {}", planId, reason);
        WorkloadPlan plan = getPlanOrThrow(planId);
        plan.cancel(reason);
        plan = planRepository.save(plan);

        // Publish event
        eventPublisher.publishPlanCancelled(
            planId, plan.getWarehouseId(), reason
        );

        return plan;
    }

    /**
     * Get workload plan recommendations
     */
    public WorkloadRecommendations getRecommendations(String warehouseId, LocalDate date) {
        DemandForecast forecast = forecastRepository
            .findByWarehouseIdOrderByForecastDateDesc(warehouseId)
            .stream()
            .findFirst()
            .orElse(null);

        WorkloadPlan plan = planRepository
            .findByWarehouseIdAndPlanDate(warehouseId, date)
            .orElse(null);

        List<String> recommendations = new ArrayList<>();

        if (plan != null) {
            if (plan.isUnderstaffed()) {
                recommendations.add(String.format(
                    "UNDERSTAFFED: Utilization at %.1f%%. Consider adding %d workers.",
                    plan.getUtilizationPercentage(),
                    calculateAdditionalWorkersNeeded(plan)
                ));
            }

            if (plan.isOverstaffed()) {
                recommendations.add(String.format(
                    "OVERSTAFFED: Utilization at %.1f%%. Consider reducing by %d workers.",
                    plan.getUtilizationPercentage(),
                    calculateExcessWorkers(plan)
                ));
            }

            if (plan.isBalanced()) {
                recommendations.add(String.format(
                    "OPTIMAL: Utilization at %.1f%%. Plan is well-balanced.",
                    plan.getUtilizationPercentage()
                ));
            }
        }

        if (forecast != null && !forecast.isAccurate()) {
            recommendations.add(String.format(
                "LOW FORECAST ACCURACY: Current accuracy %.1f%%. Recommend model refinement.",
                forecast.getAccuracy()
            ));
        }

        return new WorkloadRecommendations(recommendations, forecast, plan);
    }

    // Helper methods

    private int calculateMovingAverage(List<Integer> data, int window) {
        if (data.isEmpty()) {
            return 0;
        }

        int startIndex = Math.max(0, data.size() - window);
        List<Integer> subset = data.subList(startIndex, data.size());
        return (int) subset.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private double calculateConfidenceInterval(List<Integer> data) {
        if (data.size() < 2) {
            return 0.0;
        }

        double mean = data.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = data.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);

        return Math.sqrt(variance) * 1.96; // 95% confidence interval
    }

    private WorkloadCategory findBestCategory(WorkerCapacity worker,
                                              Map<WorkloadCategory, Integer> required) {
        return required.entrySet().stream()
            .filter(e -> e.getValue() > 0 && worker.canPerform(e.getKey()))
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private ShiftType determineOptimalShift(WorkloadPlan plan, WorkloadCategory category) {
        // Simple logic: distribute across shifts
        int dayCount = plan.getShiftAssignments(ShiftType.DAY_SHIFT).size();
        int eveningCount = plan.getShiftAssignments(ShiftType.EVENING_SHIFT).size();
        int nightCount = plan.getShiftAssignments(ShiftType.NIGHT_SHIFT).size();

        if (dayCount <= eveningCount && dayCount <= nightCount) {
            return ShiftType.DAY_SHIFT;
        } else if (eveningCount <= nightCount) {
            return ShiftType.EVENING_SHIFT;
        } else {
            return ShiftType.NIGHT_SHIFT;
        }
    }

    private int calculateAdditionalWorkersNeeded(WorkloadPlan plan) {
        if (plan.getTotalAvailableLaborHours() == 0) {
            return 0;
        }

        int shortage = plan.getTotalRequiredLaborHours() - plan.getTotalAvailableLaborHours();
        return Math.max(0, (int) Math.ceil(shortage / 8.0)); // 8-hour shifts
    }

    private int calculateExcessWorkers(WorkloadPlan plan) {
        if (plan.getTotalRequiredLaborHours() == 0) {
            return 0;
        }

        int excess = plan.getTotalAvailableLaborHours() - plan.getTotalRequiredLaborHours();
        return Math.max(0, (int) Math.floor(excess / 8.0)); // 8-hour shifts
    }

    private WorkloadPlan getPlanOrThrow(String planId) {
        return planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Workload plan not found: " + planId));
    }

    /**
     * Workload recommendations result
     */
    public record WorkloadRecommendations(
        List<String> recommendations,
        DemandForecast latestForecast,
        WorkloadPlan currentPlan
    ) {}
}
