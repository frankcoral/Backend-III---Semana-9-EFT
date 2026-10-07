package com.bancoxyz.clientes.dto;

import com.bancoxyz.clientes.model.EstadoCliente;
import java.time.Instant;

public record ClienteResponse(
                Long id,
                String rut,
                String nombres,
                String apellidos,
                String email,
                String telefono,
                String direccion,
                EstadoCliente estado,
                Instant creadoEn,
                Instant actualizadoEn) {
}
