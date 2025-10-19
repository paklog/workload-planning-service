package com.paklog.wms.workload.application.service;

import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import com.paklog.wms.workload.domain.entity.WorkerCapacity;
import com.paklog.wms.workload.domain.repository.DemandForecastRepository;
import com.paklog.wms.workload.domain.repository.WorkloadPlanRepository;
import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.SkillLevel;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkloadPlanningServiceIntegrationTest {

    private static final MongoDBContainer mongoDBContainer =
        new MongoDBContainer(DockerImageName.parse("mongo:6.0.8"));

    static {
        Startables.deepStart(mongoDBContainer).join();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private WorkloadPlanningService service;

    @Autowired
    private DemandForecastRepository forecastRepository;

    @Autowired
    private WorkloadPlanRepository planRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;

    private final ConcurrentMap<String, List<CloudEvent>> publishedEvents = new ConcurrentHashMap<>();

    @BeforeEach
    void resetState() {
        forecastRepository.deleteAll();
        planRepository.deleteAll();
        publishedEvents.clear();

        Mockito.reset(kafkaTemplate);
        Mockito.lenient()
            .when(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(1);
                CloudEvent event = invocation.getArgument(2);
                publishedEvents.computeIfAbsent(key, __ -> new ArrayList<>()).add(event);
                SettableListenableFuture<SendResult<String, CloudEvent>> future =
                    new SettableListenableFuture<>();
                future.set(null);
                return future;
            });
    }

    @AfterAll
    void tearDownInfrastructure() {
        mongoDBContainer.stop();
    }

    @Test
    void shouldGenerateForecastPersistDataAndCapturePublishedEvent() {
        Map<WorkloadCategory, List<Integer>> historicalData = Map.of(
            WorkloadCategory.PICKING, List.of(100, 120, 140, 130, 150, 160, 155),
            WorkloadCategory.RECEIVING, List.of(80, 90, 95, 100, 105, 110, 120)
        );

        DemandForecast forecast = service.generateDemandForecast(
            "WH-FORECAST",
            ForecastPeriod.DAILY,
            LocalDateTime.now(),
            historicalData
        );

        DemandForecast persisted = forecastRepository.findById(forecast.getForecastId())
            .orElseThrow();
        assertThat(persisted.getDataPoints()).isNotEmpty();
        assertThat(persisted.getAccuracy()).isEqualTo(90.0);

        List<CloudEvent> events = publishedEvents.getOrDefault(forecast.getForecastId(), List.of());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getType()).isEqualTo("com.paklog.workload.forecast.generated");
    }

    @Test
    void shouldManagePlanLifecyclePersistStateAndEmitEvents() {
        Map<WorkloadCategory, Integer> plannedVolumes = new HashMap<>();
        plannedVolumes.put(WorkloadCategory.PICKING, 400);
        plannedVolumes.put(WorkloadCategory.PACKING, 200);

        WorkloadPlan created = service.createWorkloadPlan(
            "WH-PLAN",
            LocalDate.now(),
            plannedVolumes,
            "Initial plan"
        );

        service.assignWorkerToShift(
            created.getPlanId(),
            ShiftType.DAY_SHIFT,
            "worker-a",
            "Alice",
            WorkloadCategory.PICKING,
            8
        );

        List<WorkerCapacity> availableWorkers = List.of(
            buildWorker("worker-b", "Bob", SkillLevel.EXPERT),
            buildWorker("worker-c", "Cara", SkillLevel.SENIOR),
            buildWorker("worker-d", "Dan", SkillLevel.INTERMEDIATE)
        );

        WorkloadPlan optimized = service.optimizeLaborAllocation(created.getPlanId(), availableWorkers);
        assertThat(optimized.getTotalWorkersAssigned()).isGreaterThanOrEqualTo(3);

        service.approvePlan(created.getPlanId(), "manager-1");
        service.publishPlan(created.getPlanId());
        service.cancelPlan(created.getPlanId(), "Forecast adjustment");

        WorkloadPlanningService.WorkloadRecommendations recommendations = service.getRecommendations(
            "WH-PLAN", created.getPlanDate()
        );

        assertThat(recommendations.recommendations()).isNotEmpty();
        assertThat(recommendations.currentPlan()).isNotNull();
        assertThat(planRepository.findById(created.getPlanId()))
            .map(WorkloadPlan::getStatus)
            .contains(WorkloadPlan.PlanStatus.CANCELLED);

        List<CloudEvent> events = publishedEvents.getOrDefault(created.getPlanId(), List.of());
        assertThat(events).hasSizeGreaterThanOrEqualTo(5);
        String types = events.stream().map(CloudEvent::getType).toList().toString();
        assertThat(types).contains("plan.created");
        assertThat(types).contains("worker.assigned");
        assertThat(types).contains("plan.approved");
        assertThat(types).contains("plan.published");
        assertThat(types).contains("plan.cancelled");
    }

    private WorkerCapacity buildWorker(String id, String name, SkillLevel skillLevel) {
        WorkerCapacity worker = new WorkerCapacity(id, name, skillLevel, 40, true, 25.0);
        worker.setProductivityRate(WorkloadCategory.PICKING, 28.0);
        worker.setProductivityRate(WorkloadCategory.PACKING, 22.0);
        return worker;
    }
}
