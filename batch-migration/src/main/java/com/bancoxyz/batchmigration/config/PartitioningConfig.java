package com.bancoxyz.batchmigration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PartitioningConfig {

    @Bean(name = "partitionTaskExecutor")
    public ThreadPoolTaskExecutor partitionTaskExecutor(
            @Value("${batch.partition.threads:4}") int threads) {

        int poolSize = Math.max(1, threads);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(10, poolSize * 2));
        executor.setThreadNamePrefix("Partition-Thread-");
        executor.initialize();

        return executor;
    }
}
