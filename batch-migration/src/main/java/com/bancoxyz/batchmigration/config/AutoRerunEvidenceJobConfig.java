package com.bancoxyz.batchmigration.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job de evidencia EFT:
 * - Primera ejecucion: falla de forma deliberada.
 * - Re-ejecucion automatica: finaliza correctamente.
 *
 * Permite demostrar que CriticalJobAutoRestartRunner recupera un fallo critico.
 */
@Configuration
public class AutoRerunEvidenceJobConfig {

    private static final Logger log = LoggerFactory.getLogger(AutoRerunEvidenceJobConfig.class);
    private static final AtomicInteger ATTEMPTS = new AtomicInteger(0);

    @Bean
    public Step criticalAutoRerunEvidenceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("criticalAutoRerunEvidenceStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int attempt = ATTEMPTS.incrementAndGet();

                    if (attempt == 1) {
                        log.error(
                                "FALLA CRITICA SIMULADA -> intento=1, resultado=FAILED, se espera reejecucion automatica");
                        throw new IllegalStateException(
                                "Falla critica transitoria controlada para evidencia EFT");
                    }

                    log.info(
                            "RECUPERACION AUTOMATICA EXITOSA -> intento={}, resultado=COMPLETED",
                            attempt);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job criticalAutoRerunEvidenceJob(
            JobRepository jobRepository,
            Step criticalAutoRerunEvidenceStep) {

        return new JobBuilder("criticalAutoRerunEvidenceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(criticalAutoRerunEvidenceStep)
                .build();
    }
}
