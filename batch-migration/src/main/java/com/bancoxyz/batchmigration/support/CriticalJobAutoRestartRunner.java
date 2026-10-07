package com.bancoxyz.batchmigration.support;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Re-ejecuta automaticamente una vez un Job critico que haya terminado FAILED.
 *
 * Se ejecuta despues de JobLauncherApplicationRunner (orden 0), por eso usa orden 100.
 * La re-ejecucion conserva los mismos JobParameters y reinicia la misma JobInstance.
 */
@Component
@Order(100)
public class CriticalJobAutoRestartRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriticalJobAutoRestartRunner.class);

    private static final Set<String> CRITICAL_JOBS = Set.of(
            "reporteTransaccionesDiariasJob",
            "calculoInteresesMensualesJob",
            "estadosCuentaAnualesJob",
            "criticalAutoRerunEvidenceJob"
    );

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final String configuredJobName;

    public CriticalJobAutoRestartRunner(
            JobRepository jobRepository,
            JobOperator jobOperator,
            @Value("${spring.batch.job.name:}") String configuredJobName) {
        this.jobRepository = jobRepository;
        this.jobOperator = jobOperator;
        this.configuredJobName = configuredJobName;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String jobName = configuredJobName == null ? "" : configuredJobName.trim();

        if (!CRITICAL_JOBS.contains(jobName)) {
            return;
        }

        JobInstance lastInstance = jobRepository.getLastJobInstance(jobName);
        if (lastInstance == null) {
            return;
        }

        JobExecution lastExecution = jobRepository.getLastJobExecution(lastInstance);
        if (lastExecution == null || lastExecution.getStatus() != BatchStatus.FAILED) {
            return;
        }

        List<JobExecution> executions = jobRepository.getJobExecutions(lastInstance);

        // Una unica re-ejecucion automatica por JobInstance evita bucles infinitos.
        if (executions.size() >= 2) {
            log.error(
                    "AUTO-RESTART OMITIDO -> job={}, motivo=limite de reejecucion automatica alcanzado, ejecuciones={}",
                    jobName,
                    executions.size());
            return;
        }

        log.warn(
                "AUTO-RESTART CRITICO -> job={}, executionId={}, intentoAutomatico=1/1",
                jobName,
                lastExecution.getId());

        JobExecution restartedExecution = jobOperator.restart(lastExecution);

        log.info(
                "AUTO-RESTART RESULTADO -> job={}, executionId={}, estado={}",
                jobName,
                restartedExecution.getId(),
                restartedExecution.getStatus());
    }
}
