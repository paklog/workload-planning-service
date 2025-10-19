package com.paklog.wms.workload.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wms.workload.adapter.rest.dto.AssignWorkerRequest;
import com.paklog.wms.workload.adapter.rest.dto.CreateWorkloadPlanRequest;
import com.paklog.wms.workload.adapter.rest.dto.GenerateForecastRequest;
import com.paklog.wms.workload.application.service.WorkloadPlanningService;
import com.paklog.wms.workload.domain.aggregate.DemandForecast;
import com.paklog.wms.workload.domain.aggregate.WorkloadPlan;
import com.paklog.wms.workload.domain.entity.WorkerCapacity;
import com.paklog.wms.workload.domain.valueobject.ForecastPeriod;
import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.SkillLevel;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkloadPlanningController.class)
class WorkloadPlanningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkloadPlanningService planningService;

    @Test
    void shouldGenerateForecast() throws Exception {
        DemandForecast forecast = sampleForecast();
        Mockito.when(planningService.generateDemandForecast(anyString(), any(ForecastPeriod.class), any(LocalDateTime.class), anyMap()))
            .thenReturn(forecast);

        GenerateForecastRequest request = new GenerateForecastRequest(
            "WH-1",
            ForecastPeriod.DAILY,
            forecast.getForecastDate(),
            Map.of(WorkloadCategory.PICKING, List.of(100, 120, 140))
        );

        mockMvc.perform(post("/api/v1/workload/forecasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.forecastId").value("forecast-1"))
            .andExpect(jsonPath("$.warehouseId").value("WH-1"))
            .andExpect(jsonPath("$.dataPoints[0].category").value("PICKING"));
    }

    @Test
    void shouldGetForecastById() throws Exception {
        DemandForecast forecast = sampleForecast();
        Mockito.when(planningService.getForecast("forecast-1"))
            .thenReturn(Optional.of(forecast));

        mockMvc.perform(get("/api/v1/workload/forecasts/forecast-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.forecastId").value("forecast-1"));
    }

    @Test
    void shouldReturnNotFoundWhenForecastMissing() throws Exception {
        Mockito.when(planningService.getForecast("missing"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/workload/forecasts/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldListForecasts() throws Exception {
        Mockito.when(planningService.getForecastsByWarehouse("WH-1"))
            .thenReturn(List.of(sampleForecast()));

        mockMvc.perform(get("/api/v1/workload/forecasts")
                .param("warehouseId", "WH-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].forecastId").value("forecast-1"));
    }

    @Test
    void shouldCreatePlan() throws Exception {
        WorkloadPlan plan = samplePlan();
        Mockito.when(planningService.createWorkloadPlan(anyString(), any(LocalDate.class), anyMap(), anyString()))
            .thenReturn(plan);

        CreateWorkloadPlanRequest request = new CreateWorkloadPlanRequest(
            "WH-1",
            LocalDate.now(),
            Map.of(WorkloadCategory.PICKING, 300),
            "desc"
        );

        mockMvc.perform(post("/api/v1/workload/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.planId").value("plan-1"));
    }

    @Test
    void shouldGetPlan() throws Exception {
        WorkloadPlan plan = samplePlan();
        Mockito.when(planningService.getWorkloadPlan("plan-1"))
            .thenReturn(Optional.of(plan));

        mockMvc.perform(get("/api/v1/workload/plans/plan-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("plan-1"));
    }

    @Test
    void shouldReturnNotFoundWhenPlanMissing() throws Exception {
        Mockito.when(planningService.getWorkloadPlan("missing"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/workload/plans/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldListPlans() throws Exception {
        Mockito.when(planningService.getWorkloadPlansByWarehouse("WH-1"))
            .thenReturn(List.of(samplePlan()));

        mockMvc.perform(get("/api/v1/workload/plans")
                .param("warehouseId", "WH-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].planId").value("plan-1"));
    }

    @Test
    void shouldAssignWorker() throws Exception {
        WorkloadPlan plan = samplePlan();
        plan.assignWorkerToShift(ShiftType.DAY_SHIFT, "worker-1", "Alice", WorkloadCategory.PICKING, 8);
        Mockito.when(planningService.assignWorkerToShift(anyString(), any(ShiftType.class), anyString(), anyString(), any(WorkloadCategory.class), anyInt()))
            .thenReturn(plan);

        AssignWorkerRequest request = new AssignWorkerRequest(
            ShiftType.DAY_SHIFT,
            "worker-1",
            "Alice",
            WorkloadCategory.PICKING,
            8
        );

        mockMvc.perform(post("/api/v1/workload/plans/plan-1/workers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shiftAssignments.DAY_SHIFT[0].workerId").value("worker-1"));
    }

    @Test
    void shouldOptimizeLaborAllocation() throws Exception {
        WorkloadPlan plan = samplePlan();
        plan.assignWorkerToShift(ShiftType.DAY_SHIFT, "worker-1", "Alice", WorkloadCategory.PICKING, 8);
        Mockito.when(planningService.optimizeLaborAllocation(eq("plan-1"), anyList()))
            .thenReturn(plan);

        List<WorkerCapacity> workers = List.of(
            new WorkerCapacity("worker-1", "Alice", SkillLevel.SENIOR, 40, true, 25.0)
        );

        mockMvc.perform(post("/api/v1/workload/plans/plan-1/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workers)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shiftAssignments.DAY_SHIFT[0].workerName").value("Alice"));
    }

    @Test
    void shouldApprovePublishAndCancelPlan() throws Exception {
        WorkloadPlan plan = samplePlan();
        Mockito.when(planningService.approvePlan(eq("plan-1"), anyString())).thenReturn(plan);
        Mockito.when(planningService.publishPlan("plan-1")).thenReturn(plan);
        plan.cancel("reason");
        Mockito.when(planningService.cancelPlan("plan-1", "reason")).thenReturn(plan);

        mockMvc.perform(post("/api/v1/workload/plans/plan-1/approve")
                .header("X-User-Id", "approver"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("plan-1"));

        mockMvc.perform(post("/api/v1/workload/plans/plan-1/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("plan-1"));

        mockMvc.perform(post("/api/v1/workload/plans/plan-1/cancel")
                .param("reason", "reason"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturnRecommendations() throws Exception {
        DemandForecast forecast = sampleForecastWithHighVolume();
        Mockito.when(planningService.getForecast("forecast-1"))
            .thenReturn(Optional.of(forecast));

        mockMvc.perform(get("/api/v1/workload/recommendations")
                .param("warehouseId", "WH-1")
                .param("forecastId", "forecast-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("High staffing requirement")))
            .andExpect(jsonPath("$.suggestions[0]").value(org.hamcrest.Matchers.containsString("premium")));
    }

    private DemandForecast sampleForecast() {
        DemandForecast forecast = DemandForecast.create(
            "forecast-1",
            "WH-1",
            ForecastPeriod.DAILY,
            LocalDateTime.now()
        );
        forecast.addDataPoint(LocalDateTime.now(), WorkloadCategory.PICKING, 120, 5.0);
        forecast.updateAccuracyMetrics(92.0, 4.0, 16.0);
        return forecast;
    }

    private DemandForecast sampleForecastWithHighVolume() {
        DemandForecast forecast = sampleForecast();
        forecast.addDataPoint(LocalDateTime.now().plusHours(24), WorkloadCategory.PICKING, 3200, 5.0);
        forecast.addDataPoint(LocalDateTime.now().plusHours(24), WorkloadCategory.PACKING, 1800, 3.0);
        return forecast;
    }

    private WorkloadPlan samplePlan() {
        WorkloadPlan plan = WorkloadPlan.create("plan-1", "WH-1", LocalDate.now());
        plan.setPlannedVolume(WorkloadCategory.PICKING, 300);
        plan.setPlannedVolume(WorkloadCategory.PACKING, 150);
        return plan;
    }
}
