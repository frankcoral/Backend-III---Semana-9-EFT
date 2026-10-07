package com.bancoxyz.batchmigration.config;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batchmigration.listener.BatchRetryListener;
import com.bancoxyz.batchmigration.listener.PartitionExecutionListener;
import com.bancoxyz.batchmigration.listener.RendimientoBatchListener;
import com.bancoxyz.batchmigration.listener.TransaccionSkipListener;
import com.bancoxyz.batchmigration.model.Transaccion;
import com.bancoxyz.batchmigration.partition.CsvLineRangePartitioner;
import com.bancoxyz.batchmigration.policy.TransaccionSkipPolicy;
import com.bancoxyz.batchmigration.processor.TransaccionProcessor;
import com.bancoxyz.batchmigration.reader.TransaccionPartitionReader;

@Configuration
public class TransaccionesJobConfig {

        /*
         * SEMANA 3 - PARTICIONAMIENTO
         *
         * El archivo se divide por rangos de lineas fisicas. Cada particion tiene
         * su propio Worker Step y su propio Reader, por lo que no es necesario
         * sincronizar un unico FlatFileItemReader entre hilos.
         */
        @Bean
        public Partitioner transaccionPartitioner() {
                return new CsvLineRangePartitioner("data/movimientos_financieros_diarios.csv");
        }

        @Bean
        @StepScope
        public ItemStreamReader<Transaccion> transaccionPartitionReader(
                        @Value("#{stepExecutionContext['partitionNumber']}") Integer partitionNumber,
                        @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
                        @Value("#{stepExecutionContext['endIndex']}") Integer endIndex) {

                return new TransaccionPartitionReader(
                                partitionNumber == null ? 0 : partitionNumber,
                                startIndex == null ? 0 : startIndex,
                                endIndex == null ? -1 : endIndex);
        }

        /*
         * ITEM WRITER
         */
        @Bean(name = "transaccionJdbcWriter")
        public JdbcBatchItemWriter<Transaccion> transaccionJdbcWriter(
                        DataSource dataSource) {

                return new JdbcBatchItemWriterBuilder<Transaccion>()
                                .dataSource(dataSource)
                                .sql("""
                                                INSERT INTO transacciones_procesadas
                                                (
                                                    id,
                                                    fecha,
                                                    monto,
                                                    tipo,
                                                    anomala,
                                                    motivo_anomalia
                                                )
                                                VALUES
                                                (
                                                    :id,
                                                    :fecha,
                                                    :monto,
                                                    :tipo,
                                                    :anomala,
                                                    :motivoAnomalia
                                                )
                                                """)
                                .beanMapped()
                                .build();
        }

        /*
         * STEP 1 - LIMPIEZA
         *
         * Ademas de limpiar MySQL, reinicia el estado compartido para detectar
         * duplicados entre diferentes particiones y reinicia el CSV de errores.
         */
        @Bean
        public Step limpiarTransaccionesStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        TransaccionProcessor transaccionProcessor,
                        TransaccionSkipListener transaccionSkipListener) {

                return new StepBuilder(
                                "limpiarTransaccionesStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        jdbcTemplate.update(
                                                        "DELETE FROM resumen_transacciones");

                                        jdbcTemplate.update(
                                                        "DELETE FROM transacciones_procesadas");

                                        transaccionProcessor.reset();
                                        transaccionSkipListener.reset();

                                        System.out.println(
                                                        "Datos anteriores y estado de transacciones reiniciados.");

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * WORKER STEP
         *
         * Cada particion ejecuta esta misma definicion de Step con un
         * ExecutionContext diferente (startIndex/endIndex).
         */
        @Bean
        public Step procesarTransaccionesWorkerStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("transaccionPartitionReader") ItemStreamReader<Transaccion> transaccionPartitionReader,
                        TransaccionProcessor transaccionProcessor,
                        @Qualifier("transaccionJdbcWriter") JdbcBatchItemWriter<Transaccion> transaccionWriter,
                        TransaccionSkipPolicy transaccionSkipPolicy,
                        TransaccionSkipListener transaccionSkipListener,
                        @Qualifier("batchRetryPolicy") RetryPolicy batchRetryPolicy,
                        BatchRetryListener batchRetryListener,
                        RendimientoBatchListener rendimientoBatchListener,
                        PartitionExecutionListener partitionExecutionListener,
                        @Value("${batch.chunk-size:5}") int chunkSize) {

                return new StepBuilder(
                                "procesarTransaccionesWorkerStep",
                                jobRepository)
                                .<Transaccion, Transaccion>chunk(Math.max(1, chunkSize))
                                .transactionManager(transactionManager)
                                .reader(transaccionPartitionReader)
                                .processor(transaccionProcessor)
                                .writer(transaccionWriter)
                                .faultTolerant()
                                .skipPolicy(transaccionSkipPolicy)
                                .skipListener(transaccionSkipListener)
                                .retryPolicy(batchRetryPolicy)
                                .retryListener(batchRetryListener)
                                .listener(rendimientoBatchListener)
                                .listener(partitionExecutionListener)
                                .build();
        }

        /*
         * MANAGER / PARTITION STEP
         *
         * El Partitioner crea los ExecutionContext y el TaskExecutor distribuye
         * los Worker Steps entre los hilos disponibles.
         */
        @Bean
        public Step procesarTransaccionesPartitionStep(
                        JobRepository jobRepository,
                        @Qualifier("procesarTransaccionesWorkerStep") Step workerStep,
                        Partitioner transaccionPartitioner,
                        @Qualifier("partitionTaskExecutor") ThreadPoolTaskExecutor partitionTaskExecutor,
                        @Value("${batch.partition.grid-size:4}") int gridSize) {

                return new StepBuilder(
                                "procesarTransaccionesPartitionStep",
                                jobRepository)
                                .partitioner(
                                                "procesarTransaccionesWorkerStep",
                                                transaccionPartitioner)
                                .step(workerStep)
                                .gridSize(Math.max(1, gridSize))
                                .taskExecutor(partitionTaskExecutor)
                                .build();
        }

        /*
         * STEP 3 - RESUMEN
         */
        @Bean
        public Step resumenTransaccionesStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        @Value("${batch.chunk-size:5}") int chunkSize,
                        @Value("${batch.partition.grid-size:4}") int gridSize,
                        @Value("${batch.partition.threads:4}") int threads) {

                return new StepBuilder(
                                "resumenTransaccionesStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        Integer totalProcesadas = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COUNT(*)
                                                                        FROM transacciones_procesadas
                                                                        """,
                                                        Integer.class);

                                        Integer totalAnomalas = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COUNT(*)
                                                                        FROM transacciones_procesadas
                                                                        WHERE anomala = TRUE
                                                                        """,
                                                        Integer.class);

                                        Integer totalValidas = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COUNT(*)
                                                                        FROM transacciones_procesadas
                                                                        WHERE anomala = FALSE
                                                                        """,
                                                        Integer.class);

                                        BigDecimal totalCreditos = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COALESCE(SUM(monto), 0)
                                                                        FROM transacciones_procesadas
                                                                        WHERE anomala = FALSE
                                                                        AND tipo = 'credito'
                                                                        """,
                                                        BigDecimal.class);

                                        BigDecimal totalDebitos = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COALESCE(SUM(monto), 0)
                                                                        FROM transacciones_procesadas
                                                                        WHERE anomala = FALSE
                                                                        AND tipo = 'debito'
                                                                        """,
                                                        BigDecimal.class);

                                        jdbcTemplate.update(
                                                        """
                                                                        INSERT INTO resumen_transacciones
                                                                        (
                                                                            total_procesadas,
                                                                            total_validas,
                                                                            total_anomalas,
                                                                            total_creditos,
                                                                            total_debitos
                                                                        )
                                                                        VALUES (?, ?, ?, ?, ?)
                                                                        """,
                                                        totalProcesadas,
                                                        totalValidas,
                                                        totalAnomalas,
                                                        totalCreditos,
                                                        totalDebitos);

                                        System.out.println();
                                        System.out.println(
                                                        "======================================");
                                        System.out.println(
                                                        " RESUMEN JOB TRANSACCIONES - EFT");
                                        System.out.println(
                                                        "======================================");
                                        System.out.println(
                                                        "Registros persistidos : "
                                                                        + totalProcesadas);
                                        System.out.println(
                                                        "Registros validos      : "
                                                                        + totalValidas);
                                        System.out.println(
                                                        "Anomalias persistidas  : "
                                                                        + totalAnomalas);
                                        System.out.println(
                                                        "Total creditos         : "
                                                                        + totalCreditos);
                                        System.out.println(
                                                        "Total debitos          : "
                                                                        + totalDebitos);
                                        System.out.println(
                                                        "Chunk configurado      : "
                                                                        + Math.max(1, chunkSize));
                                        System.out.println(
                                                        "Grid size solicitado   : "
                                                                        + Math.max(1, gridSize));
                                        System.out.println(
                                                        "Threads configurados   : "
                                                                        + Math.max(1, threads));
                                        System.out.println(
                                                        "Estrategia              : PARTICIONES Manager/Worker");
                                        System.out.println(
                                                        "======================================");
                                        System.out.println();

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * JOB
         */
        @Bean
        public Job reporteTransaccionesDiariasJob(
                        JobRepository jobRepository,
                        @Qualifier("limpiarTransaccionesStep") Step limpiarTransaccionesStep,
                        @Qualifier("procesarTransaccionesPartitionStep") Step procesarTransaccionesPartitionStep,
                        @Qualifier("resumenTransaccionesStep") Step resumenTransaccionesStep,
                        RendimientoBatchListener rendimientoBatchListener) {

                return new JobBuilder(
                                "reporteTransaccionesDiariasJob",
                                jobRepository)
                                .incrementer(new RunIdIncrementer())
                                .listener(rendimientoBatchListener)
                                .start(limpiarTransaccionesStep)
                                .next(procesarTransaccionesPartitionStep)
                                .next(resumenTransaccionesStep)
                                .build();
        }
}
