package com.bancoxyz.batchmigration.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/**
 * Parser centralizado para fechas provenientes del sistema legacy.
 *
 * Semana 3 contiene fechas válidas en varios formatos. Esta utilidad
 * normaliza todos los formatos aceptados a LocalDate y mantiene como
 * inválidas las fechas imposibles (por ejemplo 2024-13-01).
 */
public final class LegacyDateParser {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT));

    private LegacyDateParser() {
    }

    public static LocalDate parse(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String fecha = value.trim();

        for (DateTimeFormatter formatter : FORMATOS) {
            try {
                return LocalDate.parse(fecha, formatter);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato legacy permitido.
            }
        }

        throw new DateTimeParseException(
                "Formato o fecha legacy invalida",
                fecha,
                0);
    }
}
