package com.bancoxyz.bffatm.dto;

public record CloudConfigDto(
        String aplicacion,
        String origen,
        String canal,
        String mensaje) {
}
