package com.bancoxyz.batchmigration.config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batchmigration.listener.BatchRetryListener;
import com.bancoxyz.batchmigration.listener.RendimientoBatchListener;

/**
 * Jobs tecnicos de evidencia para demostrar, de forma deterministica y segura,
 * la politica de Retry configurada para los procesos batch.
 *
 * Ambos escenarios reutilizan el mismo bean batchRetryPolicy y el mismo
 * BatchRetryListener utilizados por los Worker Steps del proyecto:
 * 1) retryEvidenceJob: falla una vez y luego se recupera.
 * 2) retryExhaustionEvidenceJob: falla siempre hasta agotar los reintentos.
 *
 * Ninguno modifica datos de negocio.
 */
@Configuration
public class RetryEvidenceJobConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryEvidenceJobConfig.class);

    @Bean
    public Step retryEvidenceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("batchRetryPolicy") RetryPolicy batchRetryPolicy,
            BatchRetryListener batchRetryListener) {

        AtomicBoolean itemEntregado = new AtomicBoolean(false);
        AtomicBoolean falloSimulado = new AtomicBoolean(false);

        ItemReader<String> reader = () ->
                itemEntregado.compareAndSet(false, true)
                        ? "registro-controlado-retry"
                        : null;

        ItemWriter<String> writer = chunk -> {
            if (falloSimulado.compareAndSet(false, true)) {
                log.warn(
                        "FALLO TRANSITORIO SIMULADO -> primer intento de escritura rechazado; "
                                + "Spring Batch debe reintentar el mismo chunk. items={}, hilo={}",
                        chunk.size(),
                        Thread.currentThread().getName());

                throw new TransientDataAccessResourceException(
                        "Falla transitoria controlada para evidencia de Retry");
            }

            log.info(
                    "RETRY RECUPERADO -> escritura del mismo chunk completada correctamente. "
                            + "items={}, hilo={}",
                    chunk.size(),
                    Thread.currentThread().getName());
        };

        return new StepBuilder("retryEvidenceStep", jobRepository)
                .<String, String>chunk(1)
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .retryPolicy(batchRetryPolicy)
                .retryListener(batchRetryListener)
                .build();
    }

    @Bean
    public Job retryEvidenceJob(
            JobRepository jobRepository,
            @Qualifier("retryEvidenceStep") Step retryEvidenceStep,
            RendimientoBatchListener rendimientoBatchListener) {

        return new JobBuilder("retryEvidenceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(rendimientoBatchListener)
                .start(retryEvidenceStep)
                .build();
    }

    @Bean
    public Step retryExhaustionEvidenceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("batchRetryPolicy") RetryPolicy batchRetryPolicy,
            BatchRetryListener batchRetryListener) {

        AtomicBoolean itemEntregado = new AtomicBoolean(false);

        ItemReader<String> reader = () ->
                itemEntregado.compareAndSet(false, true)
                        ? "registro-controlado-retry-agotado"
                        : null;

        ItemWriter<String> writer = chunk -> {
            log.warn(
                    "FALLO PERSISTENTE SIMULADO -> escritura rechazada deliberadamente; "
                            + "el Retry debe agotarse de forma controlada. items={}, hilo={}",
                    chunk.size(),
                    Thread.currentThread().getName());

            throw new TransientDataAccessResourceException(
                    "Falla transitoria persistente controlada para evidencia de agotamiento de Retry");
        };

        return new StepBuilder("retryExhaustionEvidenceStep", jobRepository)
                .<String, String>chunk(1)
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .retryPolicy(batchRetryPolicy)
                .retryListener(batchRetryListener)
                .build();
    }

    @Bean
    public Job retryExhaustionEvidenceJob(
            JobRepository jobRepository,
            @Qualifier("retryExhaustionEvidenceStep") Step retryExhaustionEvidenceStep,
            RendimientoBatchListener rendimientoBatchListener) {

        return new JobBuilder("retryExhaustionEvidenceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(rendimientoBatchListener)
                .start(retryExhaustionEvidenceStep)
                .build();
    }
}
