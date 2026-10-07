package com.bancoxyz.batchmigration.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;

@Configuration
public class BatchRetryConfig {

    @Bean(name = "batchRetryPolicy")
    public RetryPolicy batchRetryPolicy(
            @Value("${batch.retry.max-retries:3}") long maxRetries,
            @Value("${batch.retry.delay-ms:200}") long delayMs,
            @Value("${batch.retry.multiplier:2.0}") double multiplier,
            @Value("${batch.retry.max-delay-ms:1000}") long maxDelayMs) {

        return RetryPolicy.builder()
                .maxRetries(Math.max(0, maxRetries))
                .delay(Duration.ofMillis(Math.max(0, delayMs)))
                .multiplier(Math.max(1.0, multiplier))
                .maxDelay(Duration.ofMillis(Math.max(1, maxDelayMs)))
                .includes(TransientDataAccessException.class)
                .build();
    }
}
