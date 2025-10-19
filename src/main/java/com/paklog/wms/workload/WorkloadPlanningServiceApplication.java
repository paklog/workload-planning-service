package com.paklog.wms.workload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Workload Planning Service - WMS
 * Handles workload forecasting, labor capacity planning, and demand prediction
 *
 * Features:
 * - Demand forecasting using time series analysis
 * - Labor capacity planning and scheduling
 * - Workload optimization algorithms
 * - Integration with Wave Planning Service
 * - Real-time workload monitoring
 */
@SpringBootApplication
@EnableScheduling
public class WorkloadPlanningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkloadPlanningServiceApplication.class, args);
    }
}
