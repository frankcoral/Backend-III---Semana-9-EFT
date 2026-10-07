package com.bancoxyz.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearClienteRequest(
                @NotBlank @Size(max = 20) String rut,
                @NotBlank @Size(max = 100) String nombres,
                @NotBlank @Size(max = 100) String apellidos,
                @NotBlank @Email @Size(max = 150) String email,
                @Size(max = 30) String telefono,
                @Size(max = 250) String direccion) {
}
