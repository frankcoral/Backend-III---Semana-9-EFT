package com.bancoxyz.bffmobile.service;

import com.bancoxyz.bffmobile.client.ClientesServiceClient;
import com.bancoxyz.bffmobile.client.CuentasServiceClient;
import com.bancoxyz.bffmobile.client.PagosServiceClient;
import com.bancoxyz.bffmobile.dto.MobileClienteDto;
import com.bancoxyz.bffmobile.dto.MobileClienteResumenDto;
import com.bancoxyz.bffmobile.dto.MobileCuentaDto;
import com.bancoxyz.bffmobile.dto.MobileCuentaResumenDto;
import com.bancoxyz.bffmobile.dto.MobileResumenDto;
import com.bancoxyz.bffmobile.dto.MobileTransaccionDto;
import com.bancoxyz.bffmobile.dto.MobileTransaccionResumenDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MobileService {

    private static final Logger log = LoggerFactory.getLogger(MobileService.class);

    private final ClientesServiceClient clientesClient;
    private final CuentasServiceClient cuentasClient;
    private final PagosServiceClient pagosClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public MobileService(
            ClientesServiceClient clientesClient,
            CuentasServiceClient cuentasClient,
            PagosServiceClient pagosClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.clientesClient = clientesClient;
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public MobileResumenDto resumenCliente(String rut) {
        String accessToken = tokenActual();
        return circuitBreakerFactory.create("mobileAgregacion").run(
                () -> cargarResumen(rut, accessToken),
                this::fallback);
    }

    private MobileResumenDto cargarResumen(String rut, String accessToken) {
        MobileClienteDto cliente = clientesClient.obtenerPorRut(rut, accessToken);
        List<MobileCuentaDto> cuentas = cuentasClient.listarPorCliente(cliente.id(), accessToken);

        List<MobileTransaccionDto> todas = new ArrayList<>();
        for (MobileCuentaDto cuenta : cuentas) {
            List<MobileTransaccionDto> porCuenta = pagosClient.listarPorCuenta(cuenta.id(), accessToken);
            if (porCuenta != null) {
                todas.addAll(porCuenta);
            }
        }

        List<MobileCuentaResumenDto> cuentasLigeras = cuentas.stream()
                .map(c -> new MobileCuentaResumenDto(
                        c.id(), c.numeroCuenta(), c.tipo(), c.saldo(), c.moneda()))
                .toList();

        List<MobileTransaccionResumenDto> ultimas = todas.stream()
                .sorted(Comparator.comparing(
                        MobileTransaccionDto::procesadaEn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(t -> new MobileTransaccionResumenDto(
                        t.referencia(), t.tipo(), t.estado(), t.monto(), t.procesadaEn()))
                .toList();

        return new MobileResumenDto(
                "MOBILE",
                "Payload optimizado: datos esenciales y solo las 3 transacciones más recientes",
                new MobileClienteResumenDto(
                        cliente.id(),
                        (cliente.nombres() + " " + cliente.apellidos()).trim(),
                        cliente.estado()),
                cuentasLigeras,
                ultimas);
    }

    private MobileResumenDto fallback(Throwable throwable) {
        if (throwable instanceof RestClientResponseException ex
                && ex.getStatusCode().is4xxClientError()) {
            throw ex;
        }

        log.error("BFF_MOBILE_FALLBACK -> tipo={}, mensaje={}",
                throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);

        return new MobileResumenDto(
                "MOBILE",
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
