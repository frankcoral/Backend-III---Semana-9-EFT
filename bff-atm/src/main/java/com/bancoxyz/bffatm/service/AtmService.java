package com.bancoxyz.bffatm.service;

import com.bancoxyz.bffatm.client.CuentasServiceClient;
import com.bancoxyz.bffatm.client.PagosServiceClient;
import com.bancoxyz.bffatm.dto.CuentaAtmDto;
import com.bancoxyz.bffatm.dto.PagoRetiroResponseDto;
import com.bancoxyz.bffatm.dto.RetiroAtmResponse;
import com.bancoxyz.bffatm.error.DependenciaNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Service
public class AtmService {

    private static final Logger log = LoggerFactory.getLogger(AtmService.class);

    private final CuentasServiceClient cuentasClient;
    private final PagosServiceClient pagosClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AtmService(
            CuentasServiceClient cuentasClient,
            PagosServiceClient pagosClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public CuentaAtmDto consultarSaldo(Long cuentaId) {
        String token = tokenActual();
        return circuitBreakerFactory.create("cuentasAtm").run(
                () -> cuentasClient.obtener(cuentaId, token),
                error -> {
                    rethrowBusinessError(error);
                    log.error("ATM_CUENTAS_FALLBACK type={} message={}",
                            error.getClass().getName(), error.getMessage(), error);
                    throw new DependenciaNoDisponibleException(
                            "FALLBACK ACTIVO - cuentas-service no disponible; ATM no informa un saldo no verificado", error);
                });
    }

    public RetiroAtmResponse retirar(Long cuentaId, BigDecimal monto) {
        String token = tokenActual();

        PagoRetiroResponseDto transaccion = circuitBreakerFactory.create("pagosAtm").run(
                () -> pagosClient.retirar(cuentaId, monto, token),
                error -> {
                    rethrowBusinessError(error);
                    log.error("ATM_PAGOS_FALLBACK type={} message={}",
                            error.getClass().getName(), error.getMessage(), error);
                    throw new DependenciaNoDisponibleException(
                            "FALLBACK ACTIVO - pagos-service no disponible; retiro ATM no procesado", error);
                });

        return new RetiroAtmResponse(
                "ATM",
                transaccion.id(),
                transaccion.referencia(),
                transaccion.estado(),
                cuentaId,
                transaccion.monto(),
                transaccion.procesadaEn(),
                transaccion.eventoPublicado(),
                "Retiro procesado por pagos-service y evento publicado en Kafka");
    }

    private void rethrowBusinessError(Throwable error) {
        if (error instanceof RestClientResponseException ex
                && ex.getStatusCode().is4xxClientError()) {
            throw ex;
        }
    }

    private String tokenActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No existe un JWT OAuth2 válido en el contexto de seguridad");
    }
}
