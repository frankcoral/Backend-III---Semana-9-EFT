package com.bancoxyz.batchmigration.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.core.io.ClassPathResource;

import com.bancoxyz.batchmigration.model.CuentaInteres;

/**
 * Reader independiente por particion para intereses.csv.
 *
 * Cada Worker recibe un rango fisico de registros mediante ExecutionContext,
 * por lo que no comparte estado de lectura con los otros hilos.
 */
public class CuentaInteresPartitionReader implements ItemStreamReader<CuentaInteres> {

    private static final Logger log = LoggerFactory.getLogger(CuentaInteresPartitionReader.class);
    private static final String RESOURCE_PATH = "data/intereses_trimestrales.csv";
    private static final String POSITION_KEY = "cuentaInteresPartitionReader.position";

    private final int partitionNumber;
    private final int startIndex;
    private final int endIndex;

    private BufferedReader reader;
    private int currentIndex;

    public CuentaInteresPartitionReader(
            int partitionNumber,
            int startIndex,
            int endIndex) {

        this.partitionNumber = partitionNumber;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.currentIndex = startIndex;
    }

    @Override
    public void open(ExecutionContext executionContext) {

        int resumeIndex = executionContext.getInt(POSITION_KEY, startIndex);
        this.currentIndex = Math.max(startIndex, resumeIndex);

        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);

        try {
            this.reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            // Cabecera del CSV.
            reader.readLine();

            // Avanza hasta el primer registro de esta particion.
            for (int index = 0; index < currentIndex; index++) {
                if (reader.readLine() == null) {
                    break;
                }
            }

            log.info(
                    "READER INTERESES PARTICION ABIERTO -> particion={}, rangoIndices={}..{}, reanudaEn={}, hilo={}",
                    partitionNumber,
                    startIndex,
                    endIndex,
                    currentIndex,
                    Thread.currentThread().getName());

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo abrir el archivo de intereses para la particion " + partitionNumber,
                    exception);
        }
    }

    @Override
    public CuentaInteres read() throws Exception {

        if (currentIndex > endIndex || reader == null) {
            return null;
        }

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        int dataIndex = currentIndex;
        int physicalLine = dataIndex + 2;

        // Incrementar antes de parsear evita releer un registro defectuoso.
        currentIndex++;

        try {
            return parse(line);
        } catch (Exception exception) {
            throw new FlatFileParseException(
                    "Parsing error at line: " + physicalLine
                            + " in resource=[class path resource [" + RESOURCE_PATH + "]]",
                    exception,
                    line,
                    physicalLine);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(POSITION_KEY, currentIndex);
    }

    @Override
    public void close() {

        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo cerrar el reader de intereses de la particion " + partitionNumber,
                    exception);
        } finally {
            reader = null;
        }
    }

    private CuentaInteres parse(String line) {

        String[] fields = line.split(",", -1);

        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "Cantidad de columnas invalida. Se esperaban 5 y se recibieron " + fields.length);
        }

        CuentaInteres cuenta = new CuentaInteres();

        cuenta.setCuentaId(Long.parseLong(fields[0].trim()));
        cuenta.setNombre(fields[1].trim());

        String saldo = fields[2].trim();
        if (!saldo.isBlank()) {
            cuenta.setSaldo(new BigDecimal(saldo));
        }

        String edad = fields[3].trim();
        if (!edad.isBlank()) {
            cuenta.setEdad(Integer.valueOf(edad));
        }

        cuenta.setTipo(fields[4].trim());

        return cuenta;
    }
}
