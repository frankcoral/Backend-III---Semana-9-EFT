package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.dto.*;
import com.bancoxyz.cuentas.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService service;

    public CuentaController(CuentaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaResponse crear(@Valid @RequestBody CrearCuentaRequest request) {
        return service.crear(request);
    }

    @GetMapping("/{id}")
    public CuentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/numero/{numeroCuenta}")
    public CuentaResponse obtenerPorNumero(@PathVariable String numeroCuenta) {
        return service.obtenerPorNumero(numeroCuenta);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<CuentaResponse> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId);
    }

    @PutMapping("/{id}")
    public CuentaResponse actualizar(@PathVariable Long id,
                                     @Valid @RequestBody ActualizarCuentaRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/saldo")
    public CuentaResponse actualizarSaldo(@PathVariable Long id,
                                          @Valid @RequestBody ActualizarSaldoRequest request) {
        return service.actualizarSaldo(id, request);
    }

    @PostMapping("/{id}/cierre")
    public CuentaResponse cerrar(@PathVariable Long id) {
        return service.cerrar(id);
    }
}
