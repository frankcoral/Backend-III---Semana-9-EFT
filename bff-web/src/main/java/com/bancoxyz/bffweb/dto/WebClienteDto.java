package com.bancoxyz.bffweb.dto;

import java.time.Instant;

public record WebClienteDto(
        Long id,
        String rut,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String direccion,
        String estado,
        Instant creadoEn,
        Instant actualizadoEn) {
}
