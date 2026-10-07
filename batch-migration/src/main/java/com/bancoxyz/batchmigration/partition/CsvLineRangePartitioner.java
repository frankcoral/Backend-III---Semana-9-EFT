package com.bancoxyz.batchmigration.partition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.ClassPathResource;

public class CsvLineRangePartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(CsvLineRangePartitioner.class);

    private final String resourcePath;

    public CsvLineRangePartitioner(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        int totalRecords = contarRegistros();
        int effectiveGridSize = Math.max(1, Math.min(gridSize, Math.max(1, totalRecords)));

        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();

        if (totalRecords == 0) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("partitionNumber", 0);
            context.putInt("startIndex", 0);
            context.putInt("endIndex", -1);
            context.putInt("totalRecords", 0);
            context.putString("resourcePath", resourcePath);
            partitions.put("partition0", context);
            return partitions;
        }

        int baseSize = totalRecords / effectiveGridSize;
        int remainder = totalRecords % effectiveGridSize;
        int start = 0;

        for (int partitionNumber = 0; partitionNumber < effectiveGridSize; partitionNumber++) {

            int size = baseSize + (partitionNumber < remainder ? 1 : 0);
            int end = start + size - 1;

            ExecutionContext context = new ExecutionContext();
            context.putInt("partitionNumber", partitionNumber);
            context.putInt("startIndex", start);
            context.putInt("endIndex", end);
            context.putInt("totalRecords", totalRecords);
            context.putString("resourcePath", resourcePath);

            String partitionName = "partition" + partitionNumber;
            partitions.put(partitionName, context);

            log.info(
                    "PARTICION CREADA -> archivo={}, particion={}, rangoIndices={}..{}, registros={}",
                    resourcePath,
                    partitionName,
                    start,
                    end,
                    size);

            start = end + 1;
        }

        log.info(
                "PLAN PARTICIONAMIENTO -> archivo={}, registrosTotales={}, gridSizeSolicitado={}, particionesCreadas={}",
                resourcePath,
                totalRecords,
                gridSize,
                partitions.size());

        return partitions;
    }

    private int contarRegistros() {

        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            long lines = reader.lines().count();
            return (int) Math.max(0, lines - 1);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo contar los registros del archivo " + resourcePath,
                    exception);
        }
    }
}
