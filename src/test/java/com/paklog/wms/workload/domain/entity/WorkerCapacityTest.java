package com.paklog.wms.workload.domain.entity;

import com.paklog.wms.workload.domain.valueobject.SkillLevel;
import com.paklog.wms.workload.domain.valueobject.WorkloadCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerCapacityTest {

    @Test
    void shouldCalculateEffectiveProductivityAndOutput() {
        WorkerCapacity worker = new WorkerCapacity(
            "worker-1",
            "Alice",
            SkillLevel.SENIOR,
            40,
            true,
            28.0
        );
        worker.setProductivityRate(WorkloadCategory.PICKING, 30.0);

        double effectiveRate = worker.getEffectiveProductivityRate(WorkloadCategory.PICKING);
        assertThat(effectiveRate).isEqualTo(36.0); // 30 * 1.2

        double output = worker.calculateOutput(WorkloadCategory.PICKING, 2.5);
        assertThat(output).isEqualTo(90.0);
    }

    @Test
    void shouldEstimateLaborCostWithPremiums() {
        WorkerCapacity worker = new WorkerCapacity(
            "worker-2",
            "Bob",
            SkillLevel.INTERMEDIATE,
            32,
            false,
            22.5
        );

        double cost = worker.calculateLaborCost(6.0, 1.25);
        assertThat(cost).isEqualTo(168.75);
    }

    @Test
    void shouldDetermineCapabilitiesBasedOnSkillAndRates() {
        WorkerCapacity worker = new WorkerCapacity(
            "worker-3",
            "Charlie",
            SkillLevel.JUNIOR,
            30,
            true,
            20.0
        );
        worker.setProductivityRate(WorkloadCategory.VALUE_ADDED, 14.0);

        assertThat(worker.canPerform(WorkloadCategory.VALUE_ADDED)).isTrue();
        assertThat(worker.canPerform(WorkloadCategory.PICKING)).isTrue(); // core operation

        WorkerCapacity trainee = new WorkerCapacity(
            "worker-4",
            "Dana",
            SkillLevel.TRAINEE,
            20,
            false,
            18.0
        );

        assertThat(trainee.canPerform(WorkloadCategory.PICKING)).isFalse();
    }
}
