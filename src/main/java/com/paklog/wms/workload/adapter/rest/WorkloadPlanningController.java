package com.paklog.wms.workload.adapter.rest;

import com.paklog.wms.workload.adapter.rest.dto.*;
import com.paklog.wms.workload.application.service.WorkloadPlanningService;
import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import com.paklog.wms.workload.domain.entity.WorkerCapacity;
import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for workload planning and labor optimization
 */
@RestController
@RequestMapping("/api/v1/workload")
@Tag(name = "Workload Planning", description = "Demand forecasting and labor optimization")
public class WorkloadPlanningController {

    private final WorkloadPlanningService planningService;

    public WorkloadPlanningController(WorkloadPlanningService planningService) {
        this.planningService = planningService;
    }

    /**
     * Generate demand forecast
     */
    @PostMapping("/forecasts")
    @Operation(summary = "Generate forecast", description = "Generate demand forecast using historical data")
    public ResponseEntity<ForecastResponse> generateForecast(
            @Valid @RequestBody GenerateForecastRequest request
    ) {
        DemandForecast forecast = planningService.generateDemandForecast(
            request.warehouseId(),
            request.period(),
            request.forecastDate(),
            request.historicalData()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ForecastResponse.from(forecast));
    }

    /**
     * Get forecast by ID
     */
    @GetMapping("/forecasts/{id}")
    @Operation(summary = "Get forecast", description = "Get demand forecast by ID")
    public ResponseEntity<ForecastResponse> getForecast(@PathVariable String id) {
        return planningService.getForecast(id)
            .map(ForecastResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List forecasts for warehouse
     */
    @GetMapping("/forecasts")
    @Operation(summary = "List forecasts", description = "List forecasts for warehouse")
    public ResponseEntity<List<ForecastResponse>> listForecasts(
            @RequestParam String warehouseId
    ) {
        List<DemandForecast> forecasts = planningService.getForecastsByWarehouse(warehouseId);

        return ResponseEntity.ok(
            forecasts.stream()
                .map(ForecastResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Create workload plan
     */
    @PostMapping("/plans")
    @Operation(summary = "Create plan", description = "Create a new workload plan")
    public ResponseEntity<WorkloadPlanResponse> createPlan(
            @Valid @RequestBody CreateWorkloadPlanRequest request
    ) {
        WorkloadPlan plan = planningService.createWorkloadPlan(
            request.warehouseId(),
            request.planDate(),
            request.plannedVolumes(),
            request.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(WorkloadPlanResponse.from(plan));
    }

    /**
     * Get workload plan by ID
     */
    @GetMapping("/plans/{id}")
    @Operation(summary = "Get plan", description = "Get workload plan by ID")
    public ResponseEntity<WorkloadPlanResponse> getPlan(@PathVariable String id) {
        return planningService.getWorkloadPlan(id)
            .map(WorkloadPlanResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List workload plans for warehouse
     */
    @GetMapping("/plans")
    @Operation(summary = "List plans", description = "List workload plans for warehouse")
    public ResponseEntity<List<WorkloadPlanResponse>> listPlans(
            @RequestParam String warehouseId
    ) {
        List<WorkloadPlan> plans = planningService.getWorkloadPlansByWarehouse(warehouseId);

        return ResponseEntity.ok(
            plans.stream()
                .map(WorkloadPlanResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Assign worker to shift
     */
    @PostMapping("/plans/{id}/workers")
    @Operation(summary = "Assign worker", description = "Assign worker to shift in workload plan")
    public ResponseEntity<WorkloadPlanResponse> assignWorker(
            @PathVariable String id,
            @Valid @RequestBody AssignWorkerRequest request
    ) {
        WorkloadPlan plan = planningService.assignWorkerToShift(
            id,
            request.shiftType(),
            request.workerId(),
            request.workerName(),
            request.primaryCategory(),
            request.plannedHours()
        );

        return ResponseEntity.ok(WorkloadPlanResponse.from(plan));
    }

    /**
     * Optimize labor allocation
     */
    @PostMapping("/plans/{id}/optimize")
    @Operation(summary = "Optimize labor", description = "Optimize labor allocation using available workers")
    public ResponseEntity<WorkloadPlanResponse> optimizeLaborAllocation(
            @PathVariable String id,
            @RequestBody List<WorkerCapacity> workers
    ) {
        WorkloadPlan plan = planningService.optimizeLaborAllocation(id, workers);
        return ResponseEntity.ok(WorkloadPlanResponse.from(plan));
    }

    /**
     * Approve workload plan
     */
    @PostMapping("/plans/{id}/approve")
    @Operation(summary = "Approve plan", description = "Approve workload plan for execution")
    public ResponseEntity<WorkloadPlanResponse> approvePlan(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String approvedBy
    ) {
        WorkloadPlan plan = planningService.approvePlan(id, approvedBy);
        return ResponseEntity.ok(WorkloadPlanResponse.from(plan));
    }

    /**
     * Publish workload plan
     */
    @PostMapping("/plans/{id}/publish")
    @Operation(summary = "Publish plan", description = "Publish approved plan to workers")
    public ResponseEntity<WorkloadPlanResponse> publishPlan(@PathVariable String id) {
        WorkloadPlan plan = planningService.publishPlan(id);
        return ResponseEntity.ok(WorkloadPlanResponse.from(plan));
    }

    /**
     * Cancel workload plan
     */
    @PostMapping("/plans/{id}/cancel")
    @Operation(summary = "Cancel plan", description = "Cancel workload plan")
    public ResponseEntity<WorkloadPlanResponse> cancelPlan(
            @PathVariable String id,
            @RequestParam String reason
    ) {
        WorkloadPlan plan = planningService.cancelPlan(id, reason);
        return ResponseEntity.ok(WorkloadPlanResponse.from(plan));
    }

    /**
     * Get workload recommendations
     */
    @GetMapping("/recommendations")
    @Operation(summary = "Get recommendations", description = "Get workload planning recommendations")
    public ResponseEntity<WorkloadRecommendationResponse> getRecommendations(
            @RequestParam String warehouseId,
            @RequestParam String forecastId
    ) {
        DemandForecast forecast = planningService.getForecast(forecastId)
            .orElseThrow(() -> new IllegalArgumentException("Forecast not found: " + forecastId));

        // Build recommendations based on forecast
        Map<WorkloadCategory, WorkloadRecommendationResponse.CategoryRecommendation> categoryRecs =
            new HashMap<>();

        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // Analyze each category
        for (WorkloadCategory category : WorkloadCategory.values()) {
            int forecastedVolume = forecast.getDataPoints().stream()
                .filter(dp -> dp.category() == category)
                .mapToInt(dp -> dp.forecastedVolume())
                .sum();

            int requiredWorkers = category.calculateRequiredWorkers(forecastedVolume, 8);
            double requiredHours = category.calculateLaborHours(forecastedVolume);

            categoryRecs.put(category, new WorkloadRecommendationResponse.CategoryRecommendation(
                category,
                forecastedVolume,
                requiredWorkers,
                0, // Would come from actual worker assignments
                requiredWorkers,
                requiredHours
            ));

            if (requiredWorkers > 10) {
                warnings.add(String.format("High staffing requirement for %s: %d workers",
                    category.name(), requiredWorkers));
            }
        }

        // Build shift recommendations
        Map<ShiftType, WorkloadRecommendationResponse.ShiftRecommendation> shiftRecs = new HashMap<>();
        for (ShiftType shift : ShiftType.values()) {
            int totalRequired = categoryRecs.values().stream()
                .mapToInt(cr -> cr.requiredWorkers())
                .sum() / ShiftType.values().length;

            shiftRecs.put(shift, new WorkloadRecommendationResponse.ShiftRecommendation(
                shift,
                totalRequired,
                0,
                totalRequired,
                0.0
            ));

            if (shift.isNightShift() || shift.isWeekendShift()) {
                suggestions.add(String.format("Consider %s premium (%.0f%%) for %s",
                    shift.isWeekendShift() ? "weekend" : "night",
                    (shift.getPremiumMultiplier() - 1.0) * 100,
                    shift.name()));
            }
        }

        WorkloadRecommendationResponse response = new WorkloadRecommendationResponse(
            warehouseId,
            categoryRecs,
            shiftRecs,
            85.0, // Target utilization
            "BALANCED",
            warnings,
            suggestions
        );

        return ResponseEntity.ok(response);
    }
}
