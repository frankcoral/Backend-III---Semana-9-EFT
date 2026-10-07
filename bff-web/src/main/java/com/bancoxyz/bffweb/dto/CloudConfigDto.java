package com.bancoxyz.bffweb.dto;

public record CloudConfigDto(
        String aplicacion,
        String origen,
        String canal,
        String mensaje) {
}
