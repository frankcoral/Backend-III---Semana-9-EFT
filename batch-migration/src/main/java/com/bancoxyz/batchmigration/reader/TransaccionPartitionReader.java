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

import com.bancoxyz.batchmigration.model.Transaccion;
import com.bancoxyz.batchmigration.util.LegacyDateParser;

public class TransaccionPartitionReader implements ItemStreamReader<Transaccion> {

    private static final Logger log = LoggerFactory.getLogger(TransaccionPartitionReader.class);
    private static final String RESOURCE_PATH = "data/movimientos_financieros_diarios.csv";
    private static final String POSITION_KEY = "transaccionPartitionReader.position";

    private final int partitionNumber;
    private final int startIndex;
    private final int endIndex;

    private BufferedReader reader;
    private int currentIndex;

    public TransaccionPartitionReader(
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

            // Avanza exactamente por lineas fisicas hasta el inicio de esta particion.
            for (int index = 0; index < currentIndex; index++) {
                if (reader.readLine() == null) {
                    break;
                }
            }

            log.info(
                    "READER PARTICION ABIERTO -> particion={}, rangoIndices={}..{}, reanudaEn={}, hilo={}",
                    partitionNumber,
                    startIndex,
                    endIndex,
                    currentIndex,
                    Thread.currentThread().getName());

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo abrir el archivo de transacciones para la particion " + partitionNumber,
                    exception);
        }
    }

    @Override
    public Transaccion read() throws Exception {

        if (currentIndex > endIndex || reader == null) {
            return null;
        }

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        int dataIndex = currentIndex;
        int physicalLine = dataIndex + 2;

        // Se incrementa antes de parsear para que un registro invalido no haga
        // que la particion lea lineas pertenecientes a otra particion.
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
                    "No se pudo cerrar el reader de la particion " + partitionNumber,
                    exception);
        } finally {
            reader = null;
        }
    }

    private Transaccion parse(String line) {

        String[] fields = line.split(",", -1);

        if (fields.length != 4) {
            throw new IllegalArgumentException(
                    "Cantidad de columnas invalida. Se esperaban 4 y se recibieron " + fields.length);
        }

        Transaccion transaccion = new Transaccion();

        transaccion.setId(Long.parseLong(fields[0].trim()));

        String fecha = fields[1].trim();
        if (!fecha.isBlank()) {
            transaccion.setFecha(LegacyDateParser.parse(fecha));
        }

        String monto = fields[2].trim();
        if (!monto.isBlank()) {
            transaccion.setMonto(new BigDecimal(monto));
        }

        transaccion.setTipo(fields[3].trim());

        return transaccion;
    }
}
