package com.paklog.wms.workload.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ValueObjectsTest {

    @Test
    void shiftTypePremiumsAndFlags() {
        assertThat(ShiftType.NIGHT_SHIFT.isNightShift()).isTrue();
        assertThat(ShiftType.NIGHT_SHIFT.getPremiumMultiplier()).isEqualTo(1.25);

        assertThat(ShiftType.WEEKEND_DAY.isWeekendShift()).isTrue();
        assertThat(ShiftType.WEEKEND_DAY.getPremiumMultiplier()).isEqualTo(1.50);

        assertThat(ShiftType.DAY_SHIFT.isNightShift()).isFalse();
        assertThat(ShiftType.DAY_SHIFT.isWeekendShift()).isFalse();
        assertThat(ShiftType.DAY_SHIFT.getPremiumMultiplier()).isEqualTo(1.0);
    }

    @Test
    void skillLevelCapabilities() {
        assertThat(SkillLevel.SENIOR.canTrainOthers()).isTrue();
        assertThat(SkillLevel.EXPERT.canLeadTeam()).isTrue();
        assertThat(SkillLevel.JUNIOR.canLeadTeam()).isFalse();

        double effectiveRate = SkillLevel.EXPERT.calculateEffectiveRate(20.0);
        assertThat(effectiveRate).isEqualTo(28.0);
    }

    @Test
    void forecastPeriodDurations() {
        assertThat(ForecastPeriod.HOURLY.getHoursPerPeriod()).isEqualTo(1);
        assertThat(ForecastPeriod.DAILY.getPeriodsAhead()).isGreaterThan(0);
        assertThat(ForecastPeriod.MONTHLY.getTotalForecastHours())
            .isGreaterThan(ForecastPeriod.WEEKLY.getTotalForecastHours());
        assertThat(ForecastPeriod.HOURLY.isShortTerm()).isTrue();
        assertThat(ForecastPeriod.MONTHLY.isLongTerm()).isTrue();
    }
}
