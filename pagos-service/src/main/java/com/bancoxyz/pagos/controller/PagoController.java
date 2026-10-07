package com.bancoxyz.pagos.controller;

import com.bancoxyz.pagos.dto.*;
import com.bancoxyz.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping("/depositos")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionResponse deposito(@Valid @RequestBody DepositoRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.depositar(request, authorization);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionResponse pago(@Valid @RequestBody PagoRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.pagar(request, authorization);
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionResponse transferencia(@Valid @RequestBody TransferenciaRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.transferir(request, authorization);
    }

    @PostMapping("/retiros")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionResponse retiro(@Valid @RequestBody RetiroRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.retirar(request, authorization);
    }

    @GetMapping("/{id}")
    public TransaccionResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @GetMapping
    public List<TransaccionResponse> listarPorCuenta(
            @RequestParam Long cuentaId,
            @RequestParam(defaultValue = "50") int limit) {
        return service.listarPorCuenta(cuentaId, limit);
    }
}
