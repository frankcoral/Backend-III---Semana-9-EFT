package com.bancoxyz.batchmigration.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class RendimientoBatchListener
                implements StepExecutionListener, JobExecutionListener {

        private static final Logger log = LoggerFactory.getLogger(RendimientoBatchListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {

                log.info(
                                "============================================================");
                log.info(
                                "MONITOREO JOB -> nombre={}, parametros={}",
                                jobExecution.getJobInstance().getJobName(),
                                jobExecution.getJobParameters());
                log.info(
                                "============================================================");
        }

        @Override
        public void afterJob(JobExecution jobExecution) {

                long duracionMs = calcularDuracion(
                                jobExecution.getStartTime(),
                                jobExecution.getEndTime());

                Collection<StepExecution> stepsContabilizables = jobExecution.getStepExecutions()
                                .stream()
                                // En un Step particionado, el Manager agrega los contadores de sus
                                // Workers. Si se suman Manager + Workers, read/write/skip quedan
                                // duplicados. Por eso los totales del Job excluyen solamente los
                                // PartitionStep (Manager) y mantienen los Workers y los Steps normales.
                                .filter(step -> !esManagerDeParticiones(step))
                                .toList();

                long leidos = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getReadCount)
                                .sum();

                long escritos = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getWriteCount)
                                .sum();

                long omitidos = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getSkipCount)
                                .sum();

                long commits = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getCommitCount)
                                .sum();

                long rollbacks = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getRollbackCount)
                                .sum();

                long skipRead = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getReadSkipCount)
                                .sum();

                long skipProcess = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getProcessSkipCount)
                                .sum();

                long skipWrite = stepsContabilizables.stream()
                                .mapToLong(StepExecution::getWriteSkipCount)
                                .sum();

                long entradas = leidos + skipRead;

                log.info(
                                "============================================================");
                log.info(
                                "RENDIMIENTO JOB -> nombre={}, estado={}, duracion={} ms",
                                jobExecution.getJobInstance().getJobName(),
                                jobExecution.getStatus(),
                                duracionMs);
                log.info(
                                "TOTALES JOB -> entradas={}, leidosValidos={}, escritos={}, omitidos={}, "
                                                + "skipRead={}, skipProcess={}, skipWrite={}, commits={}, rollbacks={}",
                                entradas,
                                leidos,
                                escritos,
                                omitidos,
                                skipRead,
                                skipProcess,
                                skipWrite,
                                commits,
                                rollbacks);
                log.info(
                                "PARAMETROS JOB -> {}",
                                jobExecution.getJobParameters());
                log.info(
                                "============================================================");
        }

        @Override
        public void beforeStep(StepExecution stepExecution) {

                log.info(
                                "MONITOREO STEP -> inicio={}, step={}",
                                stepExecution.getStartTime(),
                                stepExecution.getStepName());
        }

        @Override
        public ExitStatus afterStep(
                        StepExecution stepExecution) {

                long duracionMs = calcularDuracion(
                                stepExecution.getStartTime(),
                                stepExecution.getEndTime());

                log.info(
                                "RENDIMIENTO STEP -> step={}, estado={}, duracion={} ms, "
                                                + "leidos={}, escritos={}, omitidos={}, "
                                                + "skipRead={}, skipProcess={}, skipWrite={}, "
                                                + "commits={}, rollbacks={}",
                                stepExecution.getStepName(),
                                stepExecution.getStatus(),
                                duracionMs,
                                stepExecution.getReadCount(),
                                stepExecution.getWriteCount(),
                                stepExecution.getSkipCount(),
                                stepExecution.getReadSkipCount(),
                                stepExecution.getProcessSkipCount(),
                                stepExecution.getWriteSkipCount(),
                                stepExecution.getCommitCount(),
                                stepExecution.getRollbackCount());

                return null;
        }

        private boolean esManagerDeParticiones(StepExecution stepExecution) {

                String nombre = stepExecution.getStepName();
                return nombre != null && nombre.endsWith("PartitionStep");
        }

        private long calcularDuracion(
                        LocalDateTime inicio,
                        LocalDateTime fin) {

                if (inicio == null || fin == null) {
                        return 0L;
                }

                return Duration.between(inicio, fin).toMillis();
        }
}
