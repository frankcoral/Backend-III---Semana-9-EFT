package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.dto.WebClienteDetalleDto;
import com.bancoxyz.bffweb.service.WebService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web")
public class WebController {

    private final WebService service;

    public WebController(WebService service) {
        this.service = service;
    }

    /**
     * Vista completa para navegador: agrega cliente, cuentas y transacciones
     * consultando microservicios independientes mediante Eureka + LoadBalancer.
     */
    @GetMapping("/clientes/{rut}/detalle")
    public WebClienteDetalleDto detalle(@PathVariable String rut) {
        return service.detalleCliente(rut);
    }
}
