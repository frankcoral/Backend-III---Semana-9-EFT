package com.bancoxyz.bffweb.service;

import com.bancoxyz.bffweb.client.ClientesServiceClient;
import com.bancoxyz.bffweb.client.CuentasServiceClient;
import com.bancoxyz.bffweb.client.PagosServiceClient;
import com.bancoxyz.bffweb.dto.WebClienteDetalleDto;
import com.bancoxyz.bffweb.dto.WebClienteDto;
import com.bancoxyz.bffweb.dto.WebCuentaDto;
import com.bancoxyz.bffweb.dto.WebTransaccionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Service
public class WebService {

    private static final Logger log = LoggerFactory.getLogger(WebService.class);

    private final ClientesServiceClient clientesClient;
    private final CuentasServiceClient cuentasClient;
    private final PagosServiceClient pagosClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public WebService(
            ClientesServiceClient clientesClient,
            CuentasServiceClient cuentasClient,
            PagosServiceClient pagosClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.clientesClient = clientesClient;
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public WebClienteDetalleDto detalleCliente(String rut) {
        String accessToken = tokenActual();

        return circuitBreakerFactory.create("webAgregacion").run(
                () -> cargarDetalle(rut, accessToken),
                this::fallback);
    }

    private WebClienteDetalleDto cargarDetalle(String rut, String accessToken) {
        WebClienteDto cliente = clientesClient.obtenerPorRut(rut, accessToken);
        List<WebCuentaDto> cuentas = cuentasClient.listarPorCliente(cliente.id(), accessToken);
        List<WebTransaccionDto> transacciones = new ArrayList<>();

        for (WebCuentaDto cuenta : cuentas) {
            List<WebTransaccionDto> porCuenta = pagosClient.listarPorCuenta(cuenta.id(), accessToken);
            if (porCuenta != null) {
                transacciones.addAll(porCuenta);
            }
        }

        return new WebClienteDetalleDto(
                "WEB",
                "Vista completa Web: cliente, cuentas y transacciones agregadas desde microservicios independientes",
                cliente,
                cuentas,
                transacciones);
    }

    private WebClienteDetalleDto fallback(Throwable throwable) {
        if (throwable instanceof RestClientResponseException ex
                && ex.getStatusCode().is4xxClientError()) {
            throw ex;
        }

        log.error("BFF_WEB_FALLBACK -> tipo={}, mensaje={}",
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);

        return new WebClienteDetalleDto(
                "WEB",
                "FALLBACK ACTIVO - uno o más microservicios de negocio no están disponibles",
                null,
                List.of(),
                List.of());
    }

    private String tokenActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("No existe un JWT OAuth2 válido en el contexto de seguridad");
    }
}
