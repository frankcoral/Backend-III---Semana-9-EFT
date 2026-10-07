package com.bancoxyz.bffatm.controller;

import com.bancoxyz.bffatm.dto.CloudConfigDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm")
public class CloudConfigController {

    private final String applicationName;
    private final String origen;
    private final String canal;
    private final String mensaje;

    public CloudConfigController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${eft.config.origen:no-cargado}") String origen,
            @Value("${eft.config.canal:no-cargado}") String canal,
            @Value("${eft.config.mensaje:no-cargado}") String mensaje) {
        this.applicationName = applicationName;
        this.origen = origen;
        this.canal = canal;
        this.mensaje = mensaje;
    }

    @GetMapping("/cloud-config")
    public CloudConfigDto cloudConfig() {
        return new CloudConfigDto(applicationName, origen, canal, mensaje);
    }
}
