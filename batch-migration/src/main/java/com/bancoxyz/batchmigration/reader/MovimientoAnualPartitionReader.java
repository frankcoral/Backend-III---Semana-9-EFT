package com.bancoxyz.batchmigration.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.core.io.ClassPathResource;

import com.bancoxyz.batchmigration.model.MovimientoAnual;

/**
 * Reader independiente por particion para cuentas_anuales.csv.
 *
 * Cada Worker recibe un rango fisico de registros mediante ExecutionContext,
 * por lo que no comparte posicion de lectura con los otros hilos.
 */
public class MovimientoAnualPartitionReader implements ItemStreamReader<MovimientoAnual> {

    private static final Logger log = LoggerFactory.getLogger(MovimientoAnualPartitionReader.class);
    private static final String RESOURCE_PATH = "data/estados_financieros_anuales.csv";
    private static final String POSITION_KEY = "movimientoAnualPartitionReader.position";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT));

    private final int partitionNumber;
    private final int startIndex;
    private final int endIndex;

    private BufferedReader reader;
    private int currentIndex;

    public MovimientoAnualPartitionReader(
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
                    "READER ESTADOS PARTICION ABIERTO -> particion={}, rangoIndices={}..{}, reanudaEn={}, hilo={}",
                    partitionNumber,
                    startIndex,
                    endIndex,
                    currentIndex,
                    Thread.currentThread().getName());

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo abrir el archivo de estados anuales para la particion " + partitionNumber,
                    exception);
        }
    }

    @Override
    public MovimientoAnual read() throws Exception {

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
                    "No se pudo cerrar el reader de estados anuales de la particion " + partitionNumber,
                    exception);
        } finally {
            reader = null;
        }
    }

    private MovimientoAnual parse(String line) {

        String[] fields = line.split(",", -1);

        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "Cantidad de columnas invalida. Se esperaban 5 y se recibieron " + fields.length);
        }

        MovimientoAnual movimiento = new MovimientoAnual();

        movimiento.setCuentaId(Long.parseLong(fields[0].trim()));

        String fecha = fields[1].trim();
        if (!fecha.isBlank()) {
            movimiento.setFecha(parseFechaFlexible(fecha));
        }

        movimiento.setTransaccion(fields[2].trim());

        String monto = fields[3].trim();
        if (!monto.isBlank()) {
            movimiento.setMonto(new BigDecimal(monto));
        }

        movimiento.setDescripcion(fields[4].trim());

        return movimiento;
    }

    private LocalDate parseFechaFlexible(String value) {

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Prueba el siguiente formato legacy soportado.
            }
        }

        throw new DateTimeParseException(
                "Formato de fecha no soportado",
                value,
                0);
    }
}
