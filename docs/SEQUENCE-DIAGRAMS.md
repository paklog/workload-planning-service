# Workload Planning Service - Sequence Diagrams

## Overview

This document contains sequence diagrams for key operational flows in the Workload Planning Service.

## 1. Generate Demand Forecast

```mermaid
sequenceDiagram
    participant Planner
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant ForecastRepo as DemandForecastRepository
    participant EventPub as WorkloadPlanningEventPublisher
    participant Analytics

    Planner->>Controller: POST /forecasts<br/>{warehouseId, period: DAILY, historicalData}
    Controller->>Service: generateDemandForecast(params)

    Service->>Service: DemandForecast.create(warehouseId, DAILY, forecastDate)

    Service->>Service: Select model based on period:<br/>DAILY → MOVING_AVERAGE
    Service->>Service: Set model parameters:<br/>{alpha: 0.3, window: 7}

    loop For Each Workload Category
        Service->>Service: Get historical data for category
        Service->>Service: Apply forecasting algorithm

        loop For Each Period Ahead (7 days)
            Service->>Service: Calculate moving average
            Service->>Service: Calculate confidence interval
            Service->>Service: forecast.addDataPoint(timestamp, category, volume, ci)
        end
    end

    Service->>Service: Calculate accuracy metrics<br/>(MAE, MSE, accuracy%)
    Service->>Service: forecast.updateAccuracyMetrics(92.0, 5.0, 25.0)

    Service->>ForecastRepo: save(forecast)
    ForecastRepo-->>Service: DemandForecast

    Service->>EventPub: publishForecastGenerated(forecastId, model, accuracy)
    EventPub->>Analytics: ForecastGeneratedEvent

    Service-->>Controller: DemandForecast
    Controller-->>Planner: 201 Created<br/>{<br/>  forecastId,<br/>  accuracy: 92.0%,<br/>  dataPoints: [...]<br/>}
```

## 2. Create Workload Plan from Forecast

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant ForecastRepo as DemandForecastRepository
    participant PlanRepo as WorkloadPlanRepository
    participant EventPub as WorkloadPlanningEventPublisher

    Supervisor->>Controller: POST /plans<br/>{warehouseId, planDate, forecastId}
    Controller->>Service: createWorkloadPlan(warehouseId, planDate, forecast)

    Service->>ForecastRepo: findById(forecastId)
    ForecastRepo-->>Service: DemandForecast

    Service->>Service: WorkloadPlan.create(planId, warehouseId, planDate)
    Note over Service: Status: DRAFT

    loop For Each Workload Category
        Service->>Service: totalVolume = forecast.getTotalForecastedVolume(category)
        alt Volume > 0
            Service->>Service: plan.setPlannedVolume(category, volume)
            Note over Service: Recalculates required hours
        end
    end

    Service->>PlanRepo: save(plan)
    PlanRepo-->>Service: WorkloadPlan

    Service->>EventPub: publishPlanCreated(planId, warehouseId, requiredHours)

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 201 Created<br/>{<br/>  planId,<br/>  planDate,<br/>  plannedVolumes: {...},<br/>  totalRequiredHours: 120,<br/>  status: DRAFT<br/>}
```

## 3. Assign Workers to Shifts

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant EventPub as WorkloadPlanningEventPublisher

    Supervisor->>Controller: POST /plans/{id}/workers<br/>{<br/>  shift: DAY_SHIFT,<br/>  workerId: "W-001",<br/>  category: PICKING,<br/>  hours: 8<br/>}
    Controller->>Service: assignWorkerToShift(planId, DAY_SHIFT, worker, PICKING, 8)

    Service->>PlanRepo: findById(planId)
    PlanRepo-->>Service: WorkloadPlan {status: DRAFT}

    Service->>Service: plan.assignWorkerToShift(DAY_SHIFT, workerId, name, PICKING, 8)
    Note over Service: Creates ShiftAssignment<br/>Recalculates:<br/>- totalAvailableHours += 8<br/>- utilization = required/available<br/>- estimatedCost

    Service->>PlanRepo: save(plan)
    PlanRepo-->>Service: WorkloadPlan

    Service->>EventPub: publishWorkerAssigned(planId, workerId, shift, category, hours)

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 200 OK<br/>{<br/>  totalWorkers: 1,<br/>  totalAvailableHours: 8,<br/>  utilization: 150%,<br/>  status: "UNDERSTAFFED"<br/>}

    Note over Supervisor,EventPub: Assign More Workers

    loop Until Utilization Balanced
        Supervisor->>Controller: POST /plans/{id}/workers<br/>{...}
        Controller->>Service: assignWorkerToShift(...)
        Service->>Service: Recalculate metrics
        Service-->>Controller: WorkloadPlan {utilization: 95%}
    end
```

## 4. Optimize Labor Allocation

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant HR as HR System

    Supervisor->>Controller: POST /plans/{id}/optimize<br/>{availableWorkers: [...]}
    Controller->>Service: optimizeLaborAllocation(planId, availableWorkers)

    Service->>PlanRepo: findById(planId)
    PlanRepo-->>Service: WorkloadPlan

    Service->>Service: Calculate required workers per category
    loop For Each Category in Plan
        Service->>Service: volume = plannedVolumes[category]
        Service->>Service: requiredWorkers = category.calculateRequiredWorkers(volume, 8)
        Note over Service: PICKING: 500 units<br/>Rate: 50 units/hour<br/>Hours: 10<br/>Workers: 2
    end

    Service->>Service: Sort workers by skill level (EXPERT first)

    loop For Each Available Worker
        Service->>Service: Find best category for worker
        Note over Service: Match worker capabilities<br/>with required categories

        alt Worker Can Perform Category
            Service->>Service: Determine optimal shift<br/>(balance across shifts)
            Service->>Service: plan.assignWorkerToShift(shift, worker, category, 8)
            Service->>Service: Decrease required count
        end
    end

    Service->>PlanRepo: save(plan)
    PlanRepo-->>Service: WorkloadPlan

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 200 OK<br/>{<br/>  totalWorkers: 15,<br/>  utilization: 94.5%,<br/>  shiftDistribution: {<br/>    DAY: 6,<br/>    EVENING: 5,<br/>    NIGHT: 4<br/>  }<br/>}
```

## 5. Approve and Publish Plan

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant EventPub as WorkloadPlanningEventPublisher
    participant HR as HR System
    participant Workers

    Note over Supervisor,Workers: Approve Plan

    Supervisor->>Controller: POST /plans/{id}/approve<br/>{approvedBy: "SUPERVISOR-001"}
    Controller->>Service: approvePlan(planId, "SUPERVISOR-001")

    Service->>PlanRepo: findById(planId)
    PlanRepo-->>Service: WorkloadPlan {status: DRAFT}

    Service->>Service: plan.approve()
    Note over Service: Validates:<br/>- Status is DRAFT<br/>- Has worker assignments<br/>Transition: DRAFT → APPROVED

    Service->>PlanRepo: save(plan)
    PlanRepo-->>Service: WorkloadPlan

    Service->>EventPub: publishPlanApproved(planId, approvedBy, workers, utilization)
    EventPub->>HR: PlanApprovedEvent

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 200 OK {status: APPROVED}

    Note over Supervisor,Workers: Publish Plan

    Supervisor->>Controller: POST /plans/{id}/publish
    Controller->>Service: publishPlan(planId)

    Service->>PlanRepo: findById(planId)
    PlanRepo-->>Service: WorkloadPlan {status: APPROVED}

    Service->>Service: plan.publish()
    Note over Service: Transition: APPROVED → PUBLISHED

    Service->>PlanRepo: save(plan)
    Service->>EventPub: publishPlanPublished(planId, warehouseId, date, workers)
    EventPub->>HR: PlanPublishedEvent
    EventPub->>Workers: Notify workers of assignments

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 200 OK {status: PUBLISHED}
```

## 6. Get Workload Recommendations

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant ForecastRepo as DemandForecastRepository
    participant PlanRepo as WorkloadPlanRepository

    Supervisor->>Controller: GET /recommendations?<br/>warehouseId=WH-001&date=2025-10-20
    Controller->>Service: getRecommendations(warehouseId, date)

    par Fetch Latest Data
        Service->>ForecastRepo: findByWarehouseIdOrderByForecastDateDesc(warehouseId)
        ForecastRepo-->>Service: List<DemandForecast>
        Service->>Service: Get most recent forecast
    and
        Service->>PlanRepo: findByWarehouseIdAndPlanDate(warehouseId, date)
        PlanRepo-->>Service: Optional<WorkloadPlan>
    end

    Service->>Service: Analyze plan status

    alt Plan is Understaffed
        Service->>Service: utilization < 85%
        Service->>Service: additionalWorkers = calculateAdditionalWorkersNeeded(plan)
        Service->>Service: Add recommendation:<br/>"UNDERSTAFFED: Utilization 78%. Add 3 workers."
    else Plan is Overstaffed
        Service->>Service: utilization > 110%
        Service->>Service: excessWorkers = calculateExcessWorkers(plan)
        Service->>Service: Add recommendation:<br/>"OVERSTAFFED: Utilization 115%. Reduce by 2 workers."
    else Plan is Balanced
        Service->>Service: 85% <= utilization <= 110%
        Service->>Service: Add recommendation:<br/>"OPTIMAL: Utilization 94.5%. Plan is well-balanced."
    end

    alt Forecast Accuracy Low
        Service->>Service: forecast.accuracy < 85%
        Service->>Service: Add recommendation:<br/>"LOW FORECAST ACCURACY: 72%. Recommend model refinement."
    end

    Service->>Service: Build WorkloadRecommendations(recommendations, forecast, plan)

    Service-->>Controller: WorkloadRecommendations
    Controller-->>Supervisor: 200 OK<br/>{<br/>  recommendations: [<br/>    "OPTIMAL: Utilization 94.5%...",<br/>    "Consider cross-training..."<br/>  ],<br/>  forecast: {...},<br/>  plan: {...}<br/>}
```

## 7. Cancel Workload Plan

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant EventPub as WorkloadPlanningEventPublisher
    participant Workers

    Supervisor->>Controller: POST /plans/{id}/cancel<br/>{reason: "Unexpected shutdown - equipment failure"}
    Controller->>Service: cancelPlan(planId, reason)

    Service->>PlanRepo: findById(planId)
    PlanRepo-->>Service: WorkloadPlan {status: PUBLISHED}

    Service->>Service: plan.cancel(reason)
    Note over Service: Status: * → CANCELLED<br/>Notes: reason

    Service->>PlanRepo: save(plan)
    PlanRepo-->>Service: WorkloadPlan

    Service->>EventPub: publishPlanCancelled(planId, warehouseId, reason)
    EventPub->>Workers: Notify workers of cancellation

    Service-->>Controller: WorkloadPlan
    Controller-->>Supervisor: 200 OK<br/>{<br/>  status: CANCELLED,<br/>  notes: "Unexpected shutdown..."<br/>}
```

## 8. Forecast Refresh Check

```mermaid
sequenceDiagram
    participant Scheduler
    participant Service as WorkloadPlanningService
    participant ForecastRepo as DemandForecastRepository
    participant Analytics

    Note over Scheduler,Analytics: Scheduled Job Every Hour

    Scheduler->>Service: checkForecastRefresh(warehouseId)

    Service->>ForecastRepo: findByWarehouseIdOrderByForecastDateDesc(warehouseId)
    ForecastRepo-->>Service: List<DemandForecast>

    loop For Each Active Forecast
        Service->>Service: forecast.needsRefresh()
        Note over Service: Check based on period:<br/>HOURLY: > 1 hour old<br/>DAILY: > 24 hours old

        alt Needs Refresh
            Service->>Service: Fetch updated historical data
            Service->>Service: generateDemandForecast(...)
            Note over Service: Regenerate with latest data

            Service->>ForecastRepo: save(newForecast)
            Service->>Analytics: Updated forecast available
        end
    end
```

## 9. Historical Performance Analysis

```mermaid
sequenceDiagram
    participant Analyst
    participant Controller as WorkloadPlanningController
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant TaskExecution as Task Execution Service

    Analyst->>Controller: GET /plans/warehouse/{id}/analysis?<br/>startDate=2025-10-01&endDate=2025-10-15
    Controller->>Service: analyzePlans(warehouseId, startDate, endDate)

    Service->>PlanRepo: findByWarehouseIdAndPlanDateBetween(warehouseId, start, end)
    PlanRepo-->>Service: List<WorkloadPlan>

    loop For Each Plan
        Service->>TaskExecution: getActualProductivity(warehouseId, planDate)
        TaskExecution-->>Service: ActualMetrics

        Service->>Service: Compare planned vs actual:<br/>- Planned hours vs actual hours<br/>- Planned volume vs actual volume<br/>- Utilization variance

        Service->>Service: Calculate accuracy:<br/>accuracy = (1 - abs(planned - actual) / planned) * 100
    end

    Service->>Service: Aggregate results:<br/>- Average utilization<br/>- Average accuracy<br/>- Cost efficiency<br/>- Staffing trends

    Service-->>Controller: AnalysisReport
    Controller-->>Analyst: 200 OK<br/>{<br/>  period: "2025-10-01 to 2025-10-15",<br/>  avgUtilization: 92.3%,<br/>  avgAccuracy: 88.7%,<br/>  totalLaborCost: $45,600,<br/>  recommendations: [...]<br/>}
```

## 10. Worker Productivity Tracking

```mermaid
sequenceDiagram
    participant System
    participant Service as WorkloadPlanningService
    participant PlanRepo as WorkloadPlanRepository
    participant TaskExecution as Task Execution Service
    participant HR as HR System

    Note over System,HR: Daily at End of Shift

    System->>Service: updateWorkerProductivity(warehouseId, date)

    Service->>PlanRepo: findByWarehouseIdAndPlanDate(warehouseId, date)
    PlanRepo-->>Service: WorkloadPlan {status: PUBLISHED}

    loop For Each Worker in Plan
        Service->>TaskExecution: getWorkerMetrics(workerId, date)
        TaskExecution-->>Service: {<br/>  tasksCompleted: 25,<br/>  unitsProcessed: 400,<br/>  hoursWorked: 8<br/>}

        Service->>Service: Calculate productivity:<br/>actualRate = 400 units / 8 hours = 50 units/hour<br/>expectedRate = category.standardProductivity

        Service->>Service: variance = (actualRate / expectedRate - 1) * 100
        Note over Service: +10% over expected

        alt Performance Above Expected
            Service->>HR: RecommendPerformanceReview(workerId, "Exceeds expectations")
        else Performance Below Expected
            Service->>HR: RecommendTraining(workerId, category)
        end
    end

    Service-->>System: ProductivityReport
```

## Error Scenarios

### Approve Non-Draft Plan
```mermaid
sequenceDiagram
    participant User
    participant Service as WorkloadPlanningService

    User->>Service: approvePlan(planId)
    Service->>Service: plan.approve()
    Note over Service: plan.status = PUBLISHED
    Service-->>User: IllegalStateException<br/>"Only draft plans can be approved"
```

### Insufficient Historical Data
```mermaid
sequenceDiagram
    participant User
    participant Service as WorkloadPlanningService

    User->>Service: generateDemandForecast(warehouseId, ...)
    Service->>Service: Fetch historical data
    Note over Service: Only 3 days available<br/>Minimum: 7 days required
    Service-->>User: IllegalArgumentException<br/>"Insufficient historical data: need 7 days, have 3"
```

### Assign Worker to Wrong Category
```mermaid
sequenceDiagram
    participant User
    participant Service as WorkloadPlanningService

    User->>Service: assignWorkerToShift(planId, shift, workerId, FORKLIFT_OP, 8)
    Service->>Service: Check worker.canPerform(FORKLIFT_OP)
    Note over Service: Worker capabilities: [PICKING, PACKING]<br/>Does not include FORKLIFT_OP
    Service-->>User: IllegalArgumentException<br/>"Worker W-001 not qualified for FORKLIFT_OP"
```
