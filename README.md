# Workload Planning Service

Intelligent workforce planning with demand forecasting, capacity modeling, and labor optimization for operational efficiency.

## Overview

The Workload Planning Service provides comprehensive workforce and workload planning capabilities for warehouse operations. This bounded context forecasts warehouse demand based on historical patterns and upcoming orders, calculates required labor hours across operational categories, creates optimized staffing plans by shift and zone, assigns workers to shifts based on skills and availability, monitors capacity utilization and identifies gaps, and provides workload recommendations for management decision-making. The service enables data-driven labor planning that balances service levels with cost efficiency.

## Domain-Driven Design

### Bounded Context
**Workload Planning & Labor Optimization** - Manages demand forecasting, labor capacity planning, and workforce assignment for optimal warehouse operations.

### Core Domain Model

#### Aggregates
- **WorkloadPlan** - Root aggregate representing a daily staffing plan
- **DemandForecast** - Root aggregate for demand predictions

#### Entities
- **WorkerCapacity** - Worker availability and skills
- **ShiftAssignment** - Worker-to-shift assignment record

#### Value Objects
- **WorkloadCategory** - Operation type (RECEIVING, PUTAWAY, PICKING, PACKING, SHIPPING, REPLENISHMENT, CYCLE_COUNT)
- **ShiftType** - Shift classification (DAY_SHIFT, EVENING_SHIFT, NIGHT_SHIFT, WEEKEND)
- **SkillLevel** - Worker proficiency (NOVICE, INTERMEDIATE, ADVANCED, EXPERT)
- **ForecastPeriod** - Forecast horizon (DAILY, WEEKLY, MONTHLY)
- **PlanStatus** - Plan state (DRAFT, APPROVED, PUBLISHED, CANCELLED)

#### Domain Events
- **WorkloadPlanCreatedEvent** - New staffing plan created
- **DemandForecastGeneratedEvent** - Forecast completed
- **WorkerAssignedToShiftEvent** - Worker assigned
- **WorkloadPlanApprovedEvent** - Plan approved
- **WorkloadPlanPublishedEvent** - Plan published to workers
- **CapacityGapIdentifiedEvent** - Insufficient capacity detected

### Ubiquitous Language
- **Workload Plan**: Daily staffing schedule with worker assignments
- **Demand Forecast**: Predicted workload volume by category
- **Labor Hours**: Total work hours required
- **Capacity Utilization**: Percentage of available hours used
- **Shift Assignment**: Worker allocated to specific shift
- **Skill Matching**: Aligning worker skills to task requirements
- **Understaffed**: Insufficient workers for demand
- **Overstaffed**: Excess workers relative to demand
- **Balanced Plan**: 85-110% capacity utilization
- **Labor Cost**: Estimated cost for staffing plan

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wms/workload/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   ├── WorkloadPlan.java        # Plan aggregate root
│   │   └── DemandForecast.java      # Forecast aggregate root
│   ├── entity/                      # Entities
│   │   ├── WorkerCapacity.java     # Worker skills/availability
│   │   └── ShiftAssignment.java     # Assignment record
│   ├── valueobject/                 # Value objects
│   │   ├── WorkloadCategory.java
│   │   ├── ShiftType.java
│   │   ├── SkillLevel.java
│   │   ├── ForecastPeriod.java
│   │   └── PlanStatus.java
│   ├── repository/                  # Repository interfaces
│   │   ├── WorkloadPlanRepository.java
│   │   └── DemandForecastRepository.java
│   ├── service/                     # Domain services
│   │   ├── DemandForecaster.java
│   │   └── CapacityOptimizer.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   └── WorkloadPlanningService.java
│   ├── command/                     # Commands
│   │   ├── GenerateForecastCommand.java
│   │   ├── CreateWorkloadPlanCommand.java
│   │   └── AssignWorkerCommand.java
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    │   └── WorkloadPlanningController.java
    ├── persistence/                 # MongoDB repositories
    ├── forecasting/                 # Forecasting algorithms
    │   ├── TimeSeriesForecaster.java
    │   └── MovingAverageCalculator.java
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain models for planning
- **Strategy Pattern** - Pluggable forecasting algorithms
- **Template Method Pattern** - Capacity calculation workflow
- **Event-Driven Architecture** - Plan change notifications
- **Repository Pattern** - Data access abstraction
- **Specification Pattern** - Complex capacity queries
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for plans and forecasts
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Forecasting and optimization algorithms

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven plan coordination
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/workload-planning-service.git
   cd workload-planning-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8086/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f workload-planning-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8086/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8086/v3/api-docs

### Key Endpoints

#### Demand Forecasting
- `POST /forecasts` - Generate demand forecast
- `GET /forecasts/{forecastId}` - Get forecast details
- `GET /forecasts/date/{date}` - Get forecast for specific date
- `POST /forecasts/historical-analysis` - Analyze historical patterns

#### Workload Planning
- `POST /workload-plans` - Create new workload plan
- `GET /workload-plans/{planId}` - Get plan details
- `GET /workload-plans/date/{date}` - Get plan for date
- `POST /workload-plans/{planId}/approve` - Approve plan
- `POST /workload-plans/{planId}/publish` - Publish to workers
- `POST /workload-plans/{planId}/cancel` - Cancel plan

#### Worker Assignment
- `POST /workload-plans/{planId}/assign-worker` - Assign worker to shift
- `DELETE /workload-plans/{planId}/workers/{workerId}` - Remove assignment
- `GET /workload-plans/{planId}/shift/{shift}/assignments` - Get shift assignments

#### Capacity Analysis
- `GET /workload-plans/{planId}/capacity-gap` - Identify gaps
- `GET /workload-plans/{planId}/utilization` - Get utilization metrics
- `POST /workload-plans/{planId}/recommendations` - Get optimization suggestions

## Forecasting & Planning Features

### Demand Forecasting Algorithm

**Time Series Analysis with Moving Average**

```
1. Collect historical workload data (30-90 days)
2. Calculate moving average by category
3. Apply day-of-week patterns
4. Incorporate known future events (promotions, holidays)
5. Adjust for seasonal trends
6. Generate forecast with confidence intervals
```

### Labor Hour Calculation

Each workload category has standardized labor rates:

```java
RECEIVING: 100 units/hour per worker
PUTAWAY: 80 units/hour per worker
PICKING: 60 lines/hour per worker
PACKING: 40 packages/hour per worker
SHIPPING: 120 packages/hour per worker
REPLENISHMENT: 50 pallets/hour per worker
CYCLE_COUNT: 100 locations/hour per worker
```

### Capacity Optimization

**Objective**: Minimize labor cost while meeting service levels

```
Minimize: Total Labor Cost
Subject to:
  - Labor Hours >= Forecast Demand
  - Worker Shift Constraints
  - Skill Requirements Met
  - Utilization 85% - 110%
  - Minimum/Maximum Workers per Shift
```

### Shift Assignment Strategy

Intelligent worker-to-shift assignment based on:
- **Skill matching**: Proficiency level vs task complexity
- **Availability**: Worker schedule constraints
- **Workload balance**: Even distribution across shifts
- **Cross-training goals**: Skill development opportunities
- **Seniority preferences**: Priority for senior workers
- **Cost optimization**: Minimize overtime and premium pay

## Workload Planning Workflow

```
FORECAST → CREATE_PLAN → ASSIGN_WORKERS → ANALYZE_GAPS → APPROVE → PUBLISH
```

### Plan Status Lifecycle

```
DRAFT → APPROVED → PUBLISHED
   ↓
CANCELLED
```

### Capacity Utilization Ranges

- **Understaffed** (< 85%): Insufficient workers, risk of missed service levels
- **Balanced** (85-110%): Optimal staffing, good efficiency
- **Overstaffed** (> 110%): Excess workers, unnecessary labor cost

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/workload_planning
  kafka:
    bootstrap-servers: localhost:9092

workload-planning:
  forecasting:
    historical-days: 90
    moving-average-period: 7
    confidence-level: 0.95
  capacity:
    min-utilization: 85.0
    max-utilization: 110.0
    target-utilization: 95.0
  labor-rates:
    receiving: 100
    putaway: 80
    picking: 60
    packing: 40
```

## Event Integration

### Published Events
- `com.paklog.wms.workload.plan.created.v1`
- `com.paklog.wms.workload.forecast.generated.v1`
- `com.paklog.wms.workload.worker.assigned.v1`
- `com.paklog.wms.workload.plan.approved.v1`
- `com.paklog.wms.workload.plan.published.v1`
- `com.paklog.wms.workload.capacity.gap.v1`

### Consumed Events
- `com.paklog.fulfillment.order.released.v1` - Update demand forecast
- `com.paklog.inventory.received.v1` - Factor receiving workload
- `com.paklog.wes.task.completed.v1` - Track actual productivity
- `com.paklog.warehouse.operations.completed.v1` - Historical data

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Monitoring

- **Health**: http://localhost:8086/actuator/health
- **Metrics**: http://localhost:8086/actuator/metrics
- **Prometheus**: http://localhost:8086/actuator/prometheus
- **Info**: http://localhost:8086/actuator/info

### Key Metrics
- `workload.plans.created.total` - Total plans created
- `workload.forecasts.generated.total` - Forecasts generated
- `workload.capacity.utilization.average` - Average utilization
- `workload.workers.assigned.total` - Worker assignments
- `workload.capacity.gap.percentage` - Capacity shortfall
- `workload.labor.cost.estimated` - Projected labor costs

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Optimize forecasting algorithms for accuracy
4. Maintain capacity calculation consistency
5. Balance service levels with cost efficiency
6. Write comprehensive tests including forecasting tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
