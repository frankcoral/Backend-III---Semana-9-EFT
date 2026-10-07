package com.bancoxyz.batchmigration.config;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
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
import com.bancoxyz.batchmigration.listener.MovimientoAnualSkipListener;
import com.bancoxyz.batchmigration.listener.PartitionExecutionListener;
import com.bancoxyz.batchmigration.listener.RendimientoBatchListener;
import com.bancoxyz.batchmigration.model.MovimientoAnual;
import com.bancoxyz.batchmigration.partition.CsvLineRangePartitioner;
import com.bancoxyz.batchmigration.policy.MovimientoAnualSkipPolicy;
import com.bancoxyz.batchmigration.processor.MovimientoAnualProcessor;
import com.bancoxyz.batchmigration.reader.MovimientoAnualPartitionReader;

@Configuration
public class EstadosCuentaJobConfig {

        /*
         * SEMANA 3 - PARTICIONAMIENTO DE ESTADOS ANUALES
         */
        @Bean
        public Partitioner estadosAnualesPartitioner() {
                return new CsvLineRangePartitioner("data/estados_financieros_anuales.csv");
        }

        @Bean
        @StepScope
        public MovimientoAnualPartitionReader movimientoAnualPartitionReader(
                        @Value("#{stepExecutionContext['partitionNumber']}") Integer partitionNumber,
                        @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
                        @Value("#{stepExecutionContext['endIndex']}") Integer endIndex) {

                return new MovimientoAnualPartitionReader(
                                partitionNumber == null ? 0 : partitionNumber,
                                startIndex == null ? 0 : startIndex,
                                endIndex == null ? -1 : endIndex);
        }

        /*
         * ITEM WRITER
         */
        @Bean
        public JdbcBatchItemWriter<MovimientoAnual> movimientoAnualWriter(
                        DataSource dataSource) {

                return new JdbcBatchItemWriterBuilder<MovimientoAnual>()
                                .dataSource(dataSource)
                                .sql("""
                                                INSERT INTO movimientos_anuales
                                                (
                                                    cuenta_id,
                                                    fecha,
                                                    transaccion,
                                                    monto,
                                                    descripcion,
                                                    valido,
                                                    motivo_error
                                                )
                                                VALUES
                                                (
                                                    :cuentaId,
                                                    :fecha,
                                                    :transaccion,
                                                    :monto,
                                                    :descripcion,
                                                    :valido,
                                                    :motivoError
                                                )
                                                """)
                                .beanMapped()
                                .build();
        }

        /*
         * STEP 1 - LIMPIEZA / PREPARACION
         */
        @Bean
        public Step limpiarEstadosCuentaStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        MovimientoAnualSkipListener movimientoAnualSkipListener) {

                return new StepBuilder(
                                "limpiarEstadosCuentaStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        jdbcTemplate.update("DELETE FROM estados_cuenta_anuales");
                                        jdbcTemplate.update("DELETE FROM movimientos_anuales");

                                        movimientoAnualSkipListener.reset();

                                        System.out.println(
                                                        "Datos anteriores y archivo de errores de estados anuales reiniciados.");

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * WORKER STEP
         */
        @Bean
        public Step procesarMovimientosAnualesWorkerStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("movimientoAnualPartitionReader") MovimientoAnualPartitionReader movimientoAnualPartitionReader,
                        MovimientoAnualProcessor movimientoAnualProcessor,
                        JdbcBatchItemWriter<MovimientoAnual> movimientoAnualWriter,
                        MovimientoAnualSkipPolicy movimientoAnualSkipPolicy,
                        MovimientoAnualSkipListener movimientoAnualSkipListener,
                        @Qualifier("batchRetryPolicy") RetryPolicy batchRetryPolicy,
                        BatchRetryListener batchRetryListener,
                        RendimientoBatchListener rendimientoBatchListener,
                        PartitionExecutionListener partitionExecutionListener,
                        @Value("${batch.chunk-size:5}") int chunkSize) {

                return new StepBuilder(
                                "procesarMovimientosAnualesWorkerStep",
                                jobRepository)
                                .<MovimientoAnual, MovimientoAnual>chunk(Math.max(1, chunkSize))
                                .transactionManager(transactionManager)
                                .reader(movimientoAnualPartitionReader)
                                .processor(movimientoAnualProcessor)
                                .writer(movimientoAnualWriter)
                                .faultTolerant()
                                .skipPolicy(movimientoAnualSkipPolicy)
                                .skipListener(movimientoAnualSkipListener)
                                .retryPolicy(batchRetryPolicy)
                                .retryListener(batchRetryListener)
                                .listener(rendimientoBatchListener)
                                .listener(partitionExecutionListener)
                                .build();
        }

        /*
         * MANAGER / PARTITION STEP
         */
        @Bean
        public Step procesarMovimientosAnualesPartitionStep(
                        JobRepository jobRepository,
                        @Qualifier("procesarMovimientosAnualesWorkerStep") Step workerStep,
                        Partitioner estadosAnualesPartitioner,
                        @Qualifier("partitionTaskExecutor") ThreadPoolTaskExecutor partitionTaskExecutor,
                        @Value("${batch.partition.grid-size:4}") int gridSize) {

                return new StepBuilder(
                                "procesarMovimientosAnualesPartitionStep",
                                jobRepository)
                                .partitioner(
                                                "procesarMovimientosAnualesWorkerStep",
                                                estadosAnualesPartitioner)
                                .step(workerStep)
                                .gridSize(Math.max(1, gridSize))
                                .taskExecutor(partitionTaskExecutor)
                                .build();
        }

        /*
         * STEP 3 - CONSOLIDACION ANUAL POR CUENTA
         */
        @Bean
        public Step generarEstadosCuentaStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate) {

                return new StepBuilder(
                                "generarEstadosCuentaStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        jdbcTemplate.update("""
                                                        INSERT INTO estados_cuenta_anuales
                                                        (
                                                            cuenta_id,
                                                            total_movimientos,
                                                            movimientos_validos,
                                                            movimientos_invalidos,
                                                            total_depositos,
                                                            total_cargos,
                                                            saldo_neto
                                                        )
                                                        SELECT
                                                            cuenta_id,
                                                            COUNT(*),
                                                            SUM(CASE WHEN valido = TRUE THEN 1 ELSE 0 END),
                                                            SUM(CASE WHEN valido = FALSE THEN 1 ELSE 0 END),
                                                            COALESCE(
                                                                SUM(
                                                                    CASE
                                                                        WHEN valido = TRUE
                                                                        AND transaccion = 'deposito'
                                                                        THEN monto
                                                                        ELSE 0
                                                                    END
                                                                ),
                                                                0
                                                            ),
                                                            COALESCE(
                                                                SUM(
                                                                    CASE
                                                                        WHEN valido = TRUE
                                                                        AND transaccion IN ('retiro', 'compra')
                                                                        THEN ABS(monto)
                                                                        ELSE 0
                                                                    END
                                                                ),
                                                                0
                                                            ),
                                                            COALESCE(
                                                                SUM(
                                                                    CASE
                                                                        WHEN valido = TRUE
                                                                        THEN monto
                                                                        ELSE 0
                                                                    END
                                                                ),
                                                                0
                                                            )
                                                        FROM movimientos_anuales
                                                        GROUP BY cuenta_id
                                                        """);

                                        System.out.println(
                                                        "Estados de cuenta anuales consolidados correctamente.");

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * STEP 4 - INFORME DE AUDITORIA + RESUMEN
         */
        @Bean
        public Step exportarInformeAuditoriaStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        @Value("${batch.chunk-size:5}") int chunkSize,
                        @Value("${batch.partition.grid-size:4}") int gridSize,
                        @Value("${batch.partition.threads:4}") int threads) {

                return new StepBuilder(
                                "exportarInformeAuditoriaStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        Path carpetaOutput = Paths.get("output");
                                        Files.createDirectories(carpetaOutput);

                                        Path archivo = carpetaOutput.resolve(
                                                        "estados_cuenta_anuales.csv");

                                        List<Map<String, Object>> estados = jdbcTemplate.queryForList("""
                                                        SELECT
                                                            cuenta_id,
                                                            total_movimientos,
                                                            movimientos_validos,
                                                            movimientos_invalidos,
                                                            total_depositos,
                                                            total_cargos,
                                                            saldo_neto,
                                                            fecha_generacion
                                                        FROM estados_cuenta_anuales
                                                        ORDER BY cuenta_id
                                                        """);

                                        try (BufferedWriter writer = Files.newBufferedWriter(
                                                        archivo,
                                                        StandardOpenOption.CREATE,
                                                        StandardOpenOption.TRUNCATE_EXISTING)) {

                                                writer.write(
                                                                "cuenta_id,"
                                                                                + "total_movimientos,"
                                                                                + "movimientos_validos,"
                                                                                + "movimientos_invalidos,"
                                                                                + "total_depositos,"
                                                                                + "total_cargos,"
                                                                                + "saldo_neto,"
                                                                                + "fecha_generacion");
                                                writer.newLine();

                                                for (Map<String, Object> estado : estados) {
                                                        writer.write(
                                                                        estado.get("cuenta_id")
                                                                                        + ","
                                                                                        + estado.get("total_movimientos")
                                                                                        + ","
                                                                                        + estado.get("movimientos_validos")
                                                                                        + ","
                                                                                        + estado.get("movimientos_invalidos")
                                                                                        + ","
                                                                                        + estado.get("total_depositos")
                                                                                        + ","
                                                                                        + estado.get("total_cargos")
                                                                                        + ","
                                                                                        + estado.get("saldo_neto")
                                                                                        + ","
                                                                                        + estado.get("fecha_generacion"));
                                                        writer.newLine();
                                                }
                                        }

                                        Integer totalMovimientos = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM movimientos_anuales",
                                                        Integer.class);

                                        Integer movimientosValidos = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM movimientos_anuales WHERE valido = TRUE",
                                                        Integer.class);

                                        Integer movimientosInvalidos = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM movimientos_anuales WHERE valido = FALSE",
                                                        Integer.class);

                                        Integer totalCuentas = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM estados_cuenta_anuales",
                                                        Integer.class);

                                        BigDecimal saldoNetoTotal = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COALESCE(SUM(saldo_neto), 0)
                                                                        FROM estados_cuenta_anuales
                                                                        """,
                                                        BigDecimal.class);

                                        System.out.println();
                                        System.out.println("======================================");
                                        System.out.println(" RESUMEN JOB ESTADOS ANUALES - EFT");
                                        System.out.println("======================================");
                                        System.out.println("Movimientos persistidos: " + totalMovimientos);
                                        System.out.println("Movimientos validos    : " + movimientosValidos);
                                        System.out.println("Movimientos invalidos  : " + movimientosInvalidos);
                                        System.out.println("Cuentas procesadas     : " + totalCuentas);
                                        System.out.println("Saldo neto total       : " + saldoNetoTotal);
                                        System.out.println("Chunk configurado      : " + Math.max(1, chunkSize));
                                        System.out.println("Grid size solicitado   : " + Math.max(1, gridSize));
                                        System.out.println("Threads configurados   : " + Math.max(1, threads));
                                        System.out.println("Estrategia              : PARTICIONES Manager/Worker");
                                        System.out.println("Informe generado       : " + archivo.toAbsolutePath());
                                        System.out.println("======================================");
                                        System.out.println();

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * JOB
         */
        @Bean
        public Job estadosCuentaAnualesJob(
                        JobRepository jobRepository,
                        @Qualifier("limpiarEstadosCuentaStep") Step limpiarEstadosCuentaStep,
                        @Qualifier("procesarMovimientosAnualesPartitionStep") Step procesarMovimientosAnualesPartitionStep,
                        @Qualifier("generarEstadosCuentaStep") Step generarEstadosCuentaStep,
                        @Qualifier("exportarInformeAuditoriaStep") Step exportarInformeAuditoriaStep,
                        RendimientoBatchListener rendimientoBatchListener) {

                return new JobBuilder(
                                "estadosCuentaAnualesJob",
                                jobRepository)
                                .incrementer(new RunIdIncrementer())
                                .listener(rendimientoBatchListener)
                                .start(limpiarEstadosCuentaStep)
                                .next(procesarMovimientosAnualesPartitionStep)
                                .next(generarEstadosCuentaStep)
                                .next(exportarInformeAuditoriaStep)
                                .build();
        }
}
