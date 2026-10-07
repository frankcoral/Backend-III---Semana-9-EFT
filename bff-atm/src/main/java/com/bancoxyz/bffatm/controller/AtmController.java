package com.bancoxyz.bffatm.controller;

import com.bancoxyz.bffatm.dto.CuentaAtmDto;
import com.bancoxyz.bffatm.dto.RetiroAtmRequest;
import com.bancoxyz.bffatm.dto.RetiroAtmResponse;
import com.bancoxyz.bffatm.service.AtmService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm")
public class AtmController {

    private final AtmService service;

    public AtmController(AtmService service) {
        this.service = service;
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    public CuentaAtmDto saldo(@PathVariable Long cuentaId) {
        return service.consultarSaldo(cuentaId);
    }

    @PostMapping("/cuentas/{cuentaId}/retiros")
    @ResponseStatus(HttpStatus.CREATED)
    public RetiroAtmResponse retirar(
            @PathVariable Long cuentaId,
            @Valid @RequestBody RetiroAtmRequest request) {
        return service.retirar(cuentaId, request.monto());
    }
}
