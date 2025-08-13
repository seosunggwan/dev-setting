package com.example.backend.common.controller;

import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsTestController {

    @GetMapping("/test/metrics")
    @Timed(value = "test.metrics.endpoint", description = "Time taken to return test metrics")
    public String testMetrics() {
        return "Metrics test endpoint - " + System.currentTimeMillis();
    }

    @GetMapping("/test/health")
    public String testHealth() {
        return "Health check - OK";
    }
}
