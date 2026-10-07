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
import com.bancoxyz.batchmigration.listener.CuentaInteresSkipListener;
import com.bancoxyz.batchmigration.listener.PartitionExecutionListener;
import com.bancoxyz.batchmigration.listener.RendimientoBatchListener;
import com.bancoxyz.batchmigration.model.CuentaInteres;
import com.bancoxyz.batchmigration.partition.CsvLineRangePartitioner;
import com.bancoxyz.batchmigration.policy.CuentaInteresSkipPolicy;
import com.bancoxyz.batchmigration.processor.CuentaInteresProcessor;
import com.bancoxyz.batchmigration.reader.CuentaInteresPartitionReader;

@Configuration
public class InteresesJobConfig {

        /*
         * SEMANA 3 - PARTICIONAMIENTO DE INTERESES
         */
        @Bean
        public Partitioner interesPartitioner() {
                return new CsvLineRangePartitioner("data/intereses_trimestrales.csv");
        }

        @Bean
        @StepScope
        public CuentaInteresPartitionReader cuentaInteresPartitionReader(
                        @Value("#{stepExecutionContext['partitionNumber']}") Integer partitionNumber,
                        @Value("#{stepExecutionContext['startIndex']}") Integer startIndex,
                        @Value("#{stepExecutionContext['endIndex']}") Integer endIndex) {

                return new CuentaInteresPartitionReader(
                                partitionNumber == null ? 0 : partitionNumber,
                                startIndex == null ? 0 : startIndex,
                                endIndex == null ? -1 : endIndex);
        }

        /*
         * ITEM WRITER
         *
         * Semana 3 contiene multiples registros historicos con el mismo
         * cuenta_id. Por eso la tabla usa un id tecnico autoincremental y
         * conserva cuenta_id como dato de negocio, evitando perder registros.
         */
        @Bean
        public JdbcBatchItemWriter<CuentaInteres> cuentaInteresWriter(
                        DataSource dataSource) {

                return new JdbcBatchItemWriterBuilder<CuentaInteres>()
                                .dataSource(dataSource)
                                .sql("""
                                                INSERT INTO intereses_procesados
                                                (
                                                    cuenta_id,
                                                    nombre,
                                                    saldo_inicial,
                                                    edad,
                                                    tipo,
                                                    tasa_interes,
                                                    interes_calculado,
                                                    saldo_final,
                                                    valida,
                                                    motivo_error
                                                )
                                                VALUES
                                                (
                                                    :cuentaId,
                                                    :nombre,
                                                    :saldo,
                                                    :edad,
                                                    :tipo,
                                                    :tasaInteres,
                                                    :interesCalculado,
                                                    :saldoFinal,
                                                    :valida,
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
        public Step limpiarInteresesStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        CuentaInteresSkipListener cuentaInteresSkipListener) {

                return new StepBuilder(
                                "limpiarInteresesStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        jdbcTemplate.update("DELETE FROM resumen_intereses");

                                        /*
                                         * El esquema legacy usaba cuenta_id como PK. Se recrea la tabla
                                         * con una clave tecnica para permitir cuentas repetidas del
                                         * archivo oficial sin perder registros.
                                         */
                                        jdbcTemplate.execute("DROP TABLE IF EXISTS intereses_procesados");
                                        jdbcTemplate.execute("""
                                                        CREATE TABLE intereses_procesados (
                                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                            cuenta_id BIGINT NOT NULL,
                                                            nombre VARCHAR(100),
                                                            saldo_inicial DECIMAL(15,2),
                                                            edad INT,
                                                            tipo VARCHAR(50),
                                                            tasa_interes DECIMAL(10,4),
                                                            interes_calculado DECIMAL(15,2),
                                                            saldo_final DECIMAL(15,2),
                                                            valida BOOLEAN NOT NULL,
                                                            motivo_error VARCHAR(255),
                                                            INDEX idx_intereses_cuenta_id (cuenta_id)
                                                        )
                                                        """);

                                        cuentaInteresSkipListener.reset();

                                        System.out.println(
                                                        "Datos anteriores, tabla y archivo de errores de intereses reiniciados.");

                                        return RepeatStatus.FINISHED;

                                }, transactionManager)
                                .build();
        }

        /*
         * WORKER STEP
         */
        @Bean
        public Step procesarInteresesWorkerStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("cuentaInteresPartitionReader") CuentaInteresPartitionReader cuentaInteresPartitionReader,
                        CuentaInteresProcessor cuentaInteresProcessor,
                        JdbcBatchItemWriter<CuentaInteres> cuentaInteresWriter,
                        CuentaInteresSkipPolicy cuentaInteresSkipPolicy,
                        CuentaInteresSkipListener cuentaInteresSkipListener,
                        @Qualifier("batchRetryPolicy") RetryPolicy batchRetryPolicy,
                        BatchRetryListener batchRetryListener,
                        RendimientoBatchListener rendimientoBatchListener,
                        PartitionExecutionListener partitionExecutionListener,
                        @Value("${batch.chunk-size:5}") int chunkSize) {

                return new StepBuilder(
                                "procesarInteresesWorkerStep",
                                jobRepository)
                                .<CuentaInteres, CuentaInteres>chunk(Math.max(1, chunkSize))
                                .transactionManager(transactionManager)
                                .reader(cuentaInteresPartitionReader)
                                .processor(cuentaInteresProcessor)
                                .writer(cuentaInteresWriter)
                                .faultTolerant()
                                .skipPolicy(cuentaInteresSkipPolicy)
                                .skipListener(cuentaInteresSkipListener)
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
        public Step procesarInteresesPartitionStep(
                        JobRepository jobRepository,
                        @Qualifier("procesarInteresesWorkerStep") Step workerStep,
                        Partitioner interesPartitioner,
                        @Qualifier("partitionTaskExecutor") ThreadPoolTaskExecutor partitionTaskExecutor,
                        @Value("${batch.partition.grid-size:4}") int gridSize) {

                return new StepBuilder(
                                "procesarInteresesPartitionStep",
                                jobRepository)
                                .partitioner(
                                                "procesarInteresesWorkerStep",
                                                interesPartitioner)
                                .step(workerStep)
                                .gridSize(Math.max(1, gridSize))
                                .taskExecutor(partitionTaskExecutor)
                                .build();
        }

        /*
         * STEP 3 - RESUMEN MENSUAL
         */
        @Bean
        public Step resumenInteresesStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate,
                        @Value("${batch.chunk-size:5}") int chunkSize,
                        @Value("${batch.partition.grid-size:4}") int gridSize,
                        @Value("${batch.partition.threads:4}") int threads) {

                return new StepBuilder(
                                "resumenInteresesStep",
                                jobRepository)
                                .tasklet((contribution, chunkContext) -> {

                                        Integer totalProcesadas = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM intereses_procesados",
                                                        Integer.class);

                                        Integer totalValidas = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM intereses_procesados WHERE valida = TRUE",
                                                        Integer.class);

                                        Integer totalInvalidas = jdbcTemplate.queryForObject(
                                                        "SELECT COUNT(*) FROM intereses_procesados WHERE valida = FALSE",
                                                        Integer.class);

                                        BigDecimal interesTotal = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COALESCE(SUM(interes_calculado), 0)
                                                                        FROM intereses_procesados
                                                                        WHERE valida = TRUE
                                                                        """,
                                                        BigDecimal.class);

                                        jdbcTemplate.update(
                                                        """
                                                                        INSERT INTO resumen_intereses
                                                                        (
                                                                            total_procesadas,
                                                                            total_validas,
                                                                            total_invalidas,
                                                                            interes_total
                                                                        )
                                                                        VALUES (?, ?, ?, ?)
                                                                        """,
                                                        totalProcesadas,
                                                        totalValidas,
                                                        totalInvalidas,
                                                        interesTotal);

                                        System.out.println();
                                        System.out.println("======================================");
                                        System.out.println(" RESUMEN JOB INTERESES - EFT");
                                        System.out.println("======================================");
                                        System.out.println("Registros persistidos : " + totalProcesadas);
                                        System.out.println("Registros validos      : " + totalValidas);
                                        System.out.println("Registros invalidos    : " + totalInvalidas);
                                        System.out.println("Interes total          : " + interesTotal);
                                        System.out.println("Chunk configurado      : " + Math.max(1, chunkSize));
                                        System.out.println("Grid size solicitado   : " + Math.max(1, gridSize));
                                        System.out.println("Threads configurados   : " + Math.max(1, threads));
                                        System.out.println("Estrategia              : PARTICIONES Manager/Worker");
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
        public Job calculoInteresesMensualesJob(
                        JobRepository jobRepository,
                        @Qualifier("limpiarInteresesStep") Step limpiarInteresesStep,
                        @Qualifier("procesarInteresesPartitionStep") Step procesarInteresesPartitionStep,
                        @Qualifier("resumenInteresesStep") Step resumenInteresesStep,
                        RendimientoBatchListener rendimientoBatchListener) {

                return new JobBuilder(
                                "calculoInteresesMensualesJob",
                                jobRepository)
                                .incrementer(new RunIdIncrementer())
                                .listener(rendimientoBatchListener)
                                .start(limpiarInteresesStep)
                                .next(procesarInteresesPartitionStep)
                                .next(resumenInteresesStep)
                                .build();
        }
}
