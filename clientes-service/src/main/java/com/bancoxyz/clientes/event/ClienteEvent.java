package com.bancoxyz.clientes.event;

import com.bancoxyz.clientes.model.EstadoCliente;
import java.time.Instant;
import java.util.UUID;

public record ClienteEvent(
        UUID eventId,
        String tipoEvento,
        Long clienteId,
        String rut,
        String email,
        EstadoCliente estado,
        Instant fechaHora) {
    public static ClienteEvent crear(String tipoEvento, Long clienteId, String rut, String email,
            EstadoCliente estado) {
        return new ClienteEvent(UUID.randomUUID(), tipoEvento, clienteId, rut, email, estado, Instant.now());
    }
}
