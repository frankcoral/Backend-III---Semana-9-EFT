package com.bancoxyz.bffmobile.controller;

import com.bancoxyz.bffmobile.dto.MobileResumenDto;
import com.bancoxyz.bffmobile.service.MobileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile")
public class MobileController {

    private final MobileService service;

    public MobileController(MobileService service) {
        this.service = service;
    }

    /**
     * Payload liviano para el canal móvil: cliente resumido, cuentas esenciales
     * y solo las tres transacciones más recientes.
     */
    @GetMapping("/clientes/{rut}/resumen")
    public MobileResumenDto resumen(@PathVariable String rut) {
        return service.resumenCliente(rut);
    }
}
