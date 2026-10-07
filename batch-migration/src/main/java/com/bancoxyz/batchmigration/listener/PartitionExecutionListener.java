package com.bancoxyz.batchmigration.listener;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class PartitionExecutionListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(PartitionExecutionListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {

        ExecutionContext context = stepExecution.getExecutionContext();

        if (!context.containsKey("partitionNumber")) {
            return;
        }

        log.info(
                "PARTICION INICIO -> step={}, particion={}, rangoIndices={}..{}, totalArchivo={}, hilo={}",
                stepExecution.getStepName(),
                context.getInt("partitionNumber"),
                context.getInt("startIndex"),
                context.getInt("endIndex"),
                context.getInt("totalRecords"),
                Thread.currentThread().getName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        ExecutionContext context = stepExecution.getExecutionContext();

        if (!context.containsKey("partitionNumber")) {
            return null;
        }

        long durationMs = 0L;

        if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
            durationMs = Duration.between(
                    stepExecution.getStartTime(),
                    stepExecution.getEndTime()).toMillis();
        }

        log.info(
                "PARTICION FIN -> step={}, particion={}, estado={}, duracion={} ms, leidos={}, escritos={}, omitidos={}, hilo={}",
                stepExecution.getStepName(),
                context.getInt("partitionNumber"),
                stepExecution.getStatus(),
                durationMs,
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                Thread.currentThread().getName());

        return null;
    }
}
