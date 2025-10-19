package com.paklog.wms.workload.adapter.event;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event publisher for Workload Planning Service
 * Publishes CloudEvents to Kafka for downstream consumption
 */
@Component
public class WorkloadPlanningEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WorkloadPlanningEventPublisher.class);
    private static final String SOURCE = "workload-planning-service";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;

    public WorkloadPlanningEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish demand forecast generated event
     */
    public void publishForecastGenerated(
            String forecastId,
            String warehouseId,
            String period,
            String forecastingModel,
            Double accuracy
    ) {
        Map<String, Object> data = Map.of(
            "forecastId", forecastId,
            "warehouseId", warehouseId,
            "period", period,
            "forecastingModel", forecastingModel,
            "accuracy", accuracy != null ? accuracy.toString() : "0.0"
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.forecast.generated",
            forecastId,
            data
        );

        publishEvent("workload-events", forecastId, event);
    }

    /**
     * Publish workload plan created event
     */
    public void publishPlanCreated(
            String planId,
            String warehouseId,
            String planDate,
            Integer totalRequiredHours
    ) {
        Map<String, Object> data = Map.of(
            "planId", planId,
            "warehouseId", warehouseId,
            "planDate", planDate,
            "totalRequiredHours", totalRequiredHours.toString()
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.plan.created",
            planId,
            data
        );

        publishEvent("workload-events", planId, event);
    }

    /**
     * Publish workload plan approved event
     */
    public void publishPlanApproved(
            String planId,
            String warehouseId,
            String approvedBy,
            Integer totalWorkers,
            Double utilization
    ) {
        Map<String, Object> data = Map.of(
            "planId", planId,
            "warehouseId", warehouseId,
            "approvedBy", approvedBy,
            "totalWorkers", totalWorkers.toString(),
            "utilization", utilization.toString()
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.plan.approved",
            planId,
            data
        );

        publishEvent("workload-events", planId, event);
    }

    /**
     * Publish workload plan published event
     */
    public void publishPlanPublished(
            String planId,
            String warehouseId,
            String planDate,
            Integer totalWorkers
    ) {
        Map<String, Object> data = Map.of(
            "planId", planId,
            "warehouseId", warehouseId,
            "planDate", planDate,
            "totalWorkers", totalWorkers.toString()
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.plan.published",
            planId,
            data
        );

        publishEvent("workload-events", planId, event);
    }

    /**
     * Publish workload plan cancelled event
     */
    public void publishPlanCancelled(
            String planId,
            String warehouseId,
            String reason
    ) {
        Map<String, Object> data = Map.of(
            "planId", planId,
            "warehouseId", warehouseId,
            "reason", reason
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.plan.cancelled",
            planId,
            data
        );

        publishEvent("workload-events", planId, event);
    }

    /**
     * Publish worker assigned event
     */
    public void publishWorkerAssigned(
            String planId,
            String workerId,
            String workerName,
            String shift,
            String category,
            Integer plannedHours
    ) {
        Map<String, Object> data = Map.of(
            "planId", planId,
            "workerId", workerId,
            "workerName", workerName,
            "shift", shift,
            "category", category,
            "plannedHours", plannedHours.toString()
        );

        CloudEvent event = buildEvent(
            "com.paklog.workload.worker.assigned",
            planId,
            data
        );

        publishEvent("workload-events", planId, event);
    }

    /**
     * Build CloudEvent
     */
    private CloudEvent buildEvent(String type, String subject, Map<String, Object> data) {
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(SOURCE))
            .withType(type)
            .withSubject(subject)
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(convertToJson(data).getBytes())
            .build();
    }

    /**
     * Publish event to Kafka
     */
    private void publishEvent(String topic, String key, CloudEvent event) {
        try {
            kafkaTemplate.send(topic, key, event);
            logger.info("Published event: type={}, subject={}, topic={}",
                event.getType(), event.getSubject(), topic);
        } catch (Exception e) {
            logger.error("Failed to publish event: type={}, subject={}",
                event.getType(), event.getSubject(), e);
        }
    }

    /**
     * Convert data map to JSON string
     */
    private String convertToJson(Map<String, Object> data) {
        try {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Failed to convert data to JSON", e);
            return "{}";
        }
    }
}
