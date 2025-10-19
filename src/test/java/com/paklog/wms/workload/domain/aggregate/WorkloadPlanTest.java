package com.paklog.wms.workload.domain.aggregate;

import com.paklog.wms.workload.domain.valueobject.ShiftType;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadPlanTest {

    @Test
    void shouldRecalculateMetricsWhenVolumesAndAssignmentsChange() {
        WorkloadPlan plan = WorkloadPlan.create("plan-1", "WH-1", LocalDate.now());

        plan.setPlannedVolume(WorkloadCategory.PICKING, 200); // 8 hours required (ceil)
        plan.setPlannedVolume(WorkloadCategory.PACKING, 100); // 5 hours required

        assertThat(plan.getTotalRequiredLaborHours()).isEqualTo(13);
        assertThat(plan.getTotalAvailableLaborHours()).isZero();
        assertThat(plan.getUtilizationPercentage()).isZero();

        plan.assignWorkerToShift(
            ShiftType.DAY_SHIFT,
            "worker-1",
            "Alice",
            WorkloadCategory.PICKING,
            8
        );
        plan.assignWorkerToShift(
            ShiftType.EVENING_SHIFT,
            "worker-2",
            "Bob",
            WorkloadCategory.PACKING,
            8
        );

        assertThat(plan.getTotalWorkersAssigned()).isEqualTo(2);
        assertThat(plan.getTotalHoursForShift(ShiftType.DAY_SHIFT)).isEqualTo(8);
        assertThat(plan.getTotalHoursForShift(ShiftType.EVENING_SHIFT)).isEqualTo(8);
        assertThat(plan.getTotalAvailableLaborHours()).isEqualTo(16);
        assertThat(plan.getUtilizationPercentage()).isCloseTo(81.25, withinTolerance());
        assertThat(plan.isUnderstaffed()).isTrue();

        plan.removeWorkerFromShift(ShiftType.EVENING_SHIFT, "worker-2");
        assertThat(plan.getTotalWorkersAssigned()).isEqualTo(1);
        assertThat(plan.getTotalAvailableLaborHours()).isEqualTo(8);
        assertThat(plan.getShiftAssignments(ShiftType.EVENING_SHIFT)).isEmpty();
    }

    @Test
    void shouldEnforceStatusTransitionsAndCancellation() {
        WorkloadPlan plan = WorkloadPlan.create("plan-2", "WH-2", LocalDate.now());

        assertThatThrownBy(plan::publish)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only approved plans can be published");

        plan.approve();
        assertThat(plan.getStatus()).isEqualTo(WorkloadPlan.PlanStatus.APPROVED);
        assertThatThrownBy(plan::approve)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only draft plans can be approved");

        plan.publish();
        assertThat(plan.getStatus()).isEqualTo(WorkloadPlan.PlanStatus.PUBLISHED);

        plan.cancel("Forecast changed");
        assertThat(plan.getStatus()).isEqualTo(WorkloadPlan.PlanStatus.CANCELLED);
        assertThat(plan.getNotes()).isEqualTo("Forecast changed");
    }

    private static org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(0.01);
    }
}
