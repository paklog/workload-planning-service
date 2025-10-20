---
layout: default
title: Home
---

# Workload Planning Service Documentation

Warehouse workforce planning and resource allocation service with demand forecasting and labor optimization.

## Overview

The Workload Planning Service combines demand forecasting with labor capacity planning to create optimal staffing schedules. It analyzes historical workload data, predicts future demand across multiple warehouse activities, and generates staffing plans that balance labor costs with operational requirements. The service supports multiple forecasting models and provides workforce recommendations to ensure adequate capacity.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for planning data
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **Demand Forecasting** - Predict workload volumes by category and time period
- **Labor Planning** - Generate optimal staffing schedules
- **Multi-Category Support** - Receiving, putaway, picking, packing, shipping, etc.
- **Shift Management** - Day, evening, night, and weekend shifts
- **Skill-Based Allocation** - Match workers to appropriate tasks
- **Utilization Optimization** - Balance workload across shifts and workers
- **Cost Estimation** - Calculate estimated labor costs
- **Forecast Accuracy Tracking** - Monitor and improve prediction accuracy

## Domain Model

### Aggregates
- **WorkloadPlan** - Staffing plan for specific date with shift assignments
- **DemandForecast** - Predicted workload volumes with accuracy metrics

### Entities
- **ShiftAssignment** - Worker assigned to shift and category
- **ForecastDataPoint** - Time-series forecast data point
- **WorkerCapacity** - Worker skills and availability

### Value Objects
- **ShiftType** - Shift classification and timing
- **WorkloadCategory** - Work activity classification
- **SkillLevel** - Worker proficiency level
- **ForecastPeriod** - Forecast time granularity
- **PlanStatus** - Plan lifecycle status

### Plan Lifecycle

```
DRAFT -> APPROVED -> PUBLISHED -> CANCELLED
```

## Domain Events

### Published Events
- **WorkloadPlanCreated** - New staffing plan created
- **WorkloadPlanApproved** - Plan approved by manager
- **WorkloadPlanPublished** - Plan published to workers
- **WorkloadPlanCancelled** - Plan cancelled
- **WorkerAssignedToShift** - Worker assigned to shift
- **WorkerRemovedFromShift** - Worker removed from shift
- **DemandForecastGenerated** - New forecast created
- **ForecastAccuracyUpdated** - Forecast metrics updated
- **WorkloadRecommendationCreated** - Staffing recommendation generated

### Consumed Events
- **WaveCompleted** - Actual workload data for forecasting
- **TaskCompleted** - Task metrics for productivity calculation
- **ShiftCompleted** - Labor hour actuals for comparison
- **OrderVolumeProjection** - External demand signals

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Real-time workload updates
- **Strategy Pattern** - Multiple forecasting strategies
- **Specification Pattern** - Worker assignment eligibility

## API Endpoints

### Workload Planning
- `POST /workload-plans` - Create workload plan
- `GET /workload-plans/{planId}` - Get plan details
- `PUT /workload-plans/{planId}` - Update plan
- `POST /workload-plans/{planId}/approve` - Approve plan
- `POST /workload-plans/{planId}/publish` - Publish plan
- `POST /workload-plans/{planId}/cancel` - Cancel plan
- `GET /workload-plans` - List plans with filtering

### Shift Management
- `POST /workload-plans/{planId}/shifts/{shiftType}/assign` - Assign worker
- `DELETE /workload-plans/{planId}/shifts/{shiftType}/workers/{workerId}` - Remove worker
- `GET /workload-plans/{planId}/shifts` - Get all shift assignments
- `POST /workload-plans/{planId}/optimize` - Optimize labor allocation

### Demand Forecasting
- `POST /forecasts` - Generate demand forecast
- `GET /forecasts/{forecastId}` - Get forecast details
- `GET /forecasts/latest` - Get latest forecast
- `PUT /forecasts/{forecastId}/accuracy` - Update accuracy metrics
- `GET /forecasts/{forecastId}/datapoints` - Get forecast time series

### Recommendations
- `GET /recommendations/{planId}` - Get staffing recommendations
- `GET /recommendations/workforce-needs` - Get workforce gap analysis
- `POST /recommendations/apply` - Apply recommendations to plan

### Worker Management
- `GET /workers` - List available workers
- `GET /workers/{workerId}` - Get worker details
- `PUT /workers/{workerId}/capacity` - Update worker capacity
- `GET /workers/available/{date}` - Get workers available on date

## Workload Categories

### RECEIVING
Inbound receiving operations:
- Unloading trucks
- Receipt verification
- Quantity counting
- Damage inspection
- Standard productivity: 100 units/hour

### PUTAWAY
Inventory putaway to storage:
- License plate creation
- Location assignment
- Physical put
- System confirmation
- Standard productivity: 80 units/hour

### PICKING
Order picking operations:
- Pick task execution
- Location traversal
- Item scanning
- Quantity confirmation
- Standard productivity: 60 lines/hour

### PACKING
Order packing operations:
- Carton selection
- Item packing
- Quality check
- Label application
- Standard productivity: 40 orders/hour

### SHIPPING
Outbound shipping operations:
- Manifest creation
- Carton palletization
- Truck loading
- Dispatch confirmation
- Standard productivity: 150 cartons/hour

### REPLENISHMENT
Pick location replenishment:
- Bulk location retrieval
- Forward pick fill
- Quantity adjustment
- Standard productivity: 70 units/hour

### CYCLE_COUNTING
Inventory accuracy verification:
- Location counting
- Variance investigation
- Adjustment processing
- Standard productivity: 50 locations/hour

### RETURNS
Customer return processing:
- Return receipt
- Quality inspection
- Disposition determination
- Restocking
- Standard productivity: 30 returns/hour

## Shift Types

### DAY_SHIFT
- Start: 7:00 AM
- End: 3:00 PM
- Duration: 8 hours
- Peak productivity period
- Preferred for most operations

### EVENING_SHIFT
- Start: 3:00 PM
- End: 11:00 PM
- Duration: 8 hours
- Handles afternoon order cutoffs
- Packing and shipping focus

### NIGHT_SHIFT
- Start: 11:00 PM
- End: 7:00 AM
- Duration: 8 hours
- Replenishment and cleaning
- Receiving for next day

### WEEKEND_SHIFT
- Start: 8:00 AM
- End: 4:00 PM
- Duration: 8 hours
- Weekend order processing
- Premium labor rate

## Skill Levels

### NOVICE
- 0-3 months experience
- Productivity multiplier: 0.6x
- Requires close supervision
- Limited to simple tasks

### INTERMEDIATE
- 3-12 months experience
- Productivity multiplier: 0.8x
- Moderate supervision
- Most standard tasks

### ADVANCED
- 1-3 years experience
- Productivity multiplier: 1.0x
- Minimal supervision
- All standard tasks + complex

### EXPERT
- 3+ years experience
- Productivity multiplier: 1.2x
- Can train others
- All tasks including specialized

## Demand Forecasting

### Forecast Periods

#### HOURLY
- Short-term forecasting (24 hours ahead)
- Intraday workload distribution
- Used for shift break planning

#### DAILY
- Medium-term forecasting (7 days ahead)
- Daily staffing requirements
- Primary planning horizon

#### WEEKLY
- Long-term forecasting (4 weeks ahead)
- Weekly shift schedules
- Seasonal trend analysis

#### MONTHLY
- Strategic forecasting (3 months ahead)
- Hiring and capacity planning
- Budget estimation

### Forecasting Models

#### Time Series Analysis
- Historical pattern recognition
- Seasonal adjustments
- Trend extrapolation

#### Moving Average
- Simple moving average
- Weighted moving average
- Exponential smoothing

#### Linear Regression
- Trend-based prediction
- Multiple variable correlation

#### Machine Learning
- Neural network models
- Feature engineering
- Continuous learning

### Forecast Accuracy Metrics

#### Mean Absolute Error (MAE)
Measure of average forecast error magnitude.

#### Mean Squared Error (MSE)
Measure of squared forecast errors.

#### Forecast Accuracy %
```
Accuracy = (1 - |Actual - Forecast| / Actual) × 100%
```

## Labor Planning

### Labor Hour Calculation

For each workload category:
```
Required Labor Hours = Forecasted Volume / Standard Productivity / Skill Multiplier
```

Example:
- Forecasted picking: 1000 lines
- Standard productivity: 60 lines/hour
- Average skill multiplier: 0.9
- Required hours: 1000 / 60 / 0.9 = 18.5 hours

### Worker Allocation

Allocate workers to shifts based on:
- Required labor hours by category
- Worker capabilities and skill levels
- Worker availability and preferences
- Shift coverage requirements
- Labor cost constraints

### Utilization Optimization

Balance objectives:
- **Minimize Labor Cost** - Reduce total labor hours
- **Maximize Productivity** - Optimal skill matching
- **Balance Workload** - Even distribution across shifts
- **Meet SLAs** - Ensure adequate capacity

### Staffing Status

#### Understaffed
Total available labor hours < required labor hours
- Alert sent to managers
- Recommendations for additional hiring
- Overtime approval requested

#### Balanced
Total available labor hours ≈ required labor hours (±10%)
- Optimal staffing level
- Normal operations expected

#### Overstaffed
Total available labor hours > required labor hours
- Opportunities for cross-training
- Suggested reallocation to other areas
- Voluntary time off options

## Integration Points

### Consumes Events From
- Wave Planning (wave volumes)
- Task Execution (task completion metrics)
- Pick Execution (picking productivity)
- Pack Ship (packing productivity)

### Publishes Events To
- Warehouse Analytics (planning metrics)
- HR System (shift schedules)
- Cost Accounting (labor cost estimates)
- Task Execution (capacity availability)

## Performance Considerations

### Forecast Calculation
- Asynchronous forecast generation
- Cached results for repeated queries
- Incremental forecast updates
- Historical data aggregation

### Plan Optimization
- Constraint-based optimization algorithms
- Heuristic worker assignment
- Pre-calculated productivity rates
- Cached worker capabilities

### Database Optimization
- MongoDB indexes on planDate, warehouseId, status
- Compound index on warehouseId + planDate
- Index on forecastDate for time-series queries
- TTL index on old forecasts (archive after 365 days)

## Business Rules

1. **Planning Rules**
   - Plan date must be future or today
   - Cannot modify published plans
   - Must have at least one shift assignment
   - Total labor hours must not exceed budget

2. **Assignment Rules**
   - Worker must have required capabilities
   - Worker must be available on plan date
   - Cannot exceed worker max hours per week
   - Skill level affects labor hour calculation

3. **Forecast Rules**
   - Historical data minimum: 30 days
   - Forecast must include all workload categories
   - Accuracy tracked and stored
   - Forecasts refreshed every 6 hours

4. **Approval Workflow**
   - Draft plans can be modified freely
   - Approved plans require manager authorization
   - Published plans visible to workers
   - Cancelled plans cannot be reactivated

## Metrics and KPIs

### Planning Metrics
- Forecast accuracy percentage
- Plan-to-actual variance
- Labor utilization percentage
- Cost per labor hour
- Staffing level adherence

### Workforce Metrics
- Worker productivity by skill level
- Shift coverage percentage
- Overtime hours percentage
- Cross-training effectiveness

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `forecast.history-days-required` - Minimum historical data (default: 30)
- `forecast.refresh-interval` - Forecast refresh frequency (default: 6h)
- `forecast.model` - Forecasting algorithm (time-series, moving-average, regression, ml)
- `plan.utilization.min-threshold` - Understaffed threshold % (default: 90)
- `plan.utilization.max-threshold` - Overstaffed threshold % (default: 110)
- `plan.labor-cost.hourly-rate` - Default hourly rate (default: $15.00)
- `plan.labor-cost.overtime-multiplier` - Overtime rate multiplier (default: 1.5)
- `worker.max-hours-per-week` - Maximum weekly hours (default: 40)

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: Operations Team
- **Slack**: #operations-workload-planning
