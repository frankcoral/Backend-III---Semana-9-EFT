package com.bancoxyz.pagos.client;

import com.bancoxyz.pagos.dto.ActualizarSaldoRequest;
import com.bancoxyz.pagos.dto.CuentaResumen;
import com.bancoxyz.pagos.exception.DependenciaNoDisponibleException;
import com.bancoxyz.pagos.exception.OperacionPagoException;
import com.bancoxyz.pagos.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CuentaClient {

    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CuentaClient(
            @LoadBalanced RestClient.Builder builder,
            @Value("${banco.servicios.cuentas-url:http://cuentas-service}") String cuentasUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = builder.baseUrl(cuentasUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public CuentaResumen obtener(Long cuentaId, String authorization) {
        return circuitBreakerFactory.create("cuentasService").run(
                () -> ejecutarObtener(cuentaId, authorization),
                throwable -> rethrowOrDependency("consultar", cuentaId, throwable));
    }

    public CuentaResumen actualizarSaldo(Long cuentaId, java.math.BigDecimal saldo, String authorization) {
        return circuitBreakerFactory.create("cuentasService").run(
                () -> ejecutarSaldo(cuentaId, saldo, authorization),
                throwable -> rethrowOrDependency("actualizar saldo de", cuentaId, throwable));
    }

    private CuentaResumen ejecutarObtener(Long cuentaId, String authorization) {
        try {
            return restClient.get()
                    .uri("/api/cuentas/{id}", cuentaId)
                    .headers(headers -> relay(headers, authorization))
                    .retrieve()
                    .body(CuentaResumen.class);
        } catch (RestClientResponseException ex) {
            throw traducirRespuesta(cuentaId, ex);
        } catch (RestClientException ex) {
            throw new DependenciaNoDisponibleException(
                    "Error consultando cuentas-service para la cuenta " + cuentaId, ex);
        }
    }

    private CuentaResumen ejecutarSaldo(Long cuentaId, java.math.BigDecimal saldo, String authorization) {
        try {
            return restClient.patch()
                    .uri("/api/cuentas/{id}/saldo", cuentaId)
                    .headers(headers -> relay(headers, authorization))
                    .body(new ActualizarSaldoRequest(saldo))
                    .retrieve()
                    .body(CuentaResumen.class);
        } catch (RestClientResponseException ex) {
            throw traducirRespuesta(cuentaId, ex);
        } catch (RestClientException ex) {
            throw new DependenciaNoDisponibleException(
                    "Error actualizando saldo en cuentas-service para la cuenta " + cuentaId, ex);
        }
    }

    private RuntimeException traducirRespuesta(Long cuentaId, RestClientResponseException ex) {
        if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return new RecursoNoEncontradoException("Cuenta no encontrada: " + cuentaId);
        }
        if (ex.getStatusCode().is4xxClientError()) {
            return new OperacionPagoException(
                    "cuentas-service rechazó la operación para la cuenta " + cuentaId + " (HTTP "
                            + ex.getStatusCode().value() + ")",
                    ex);
        }
        return new DependenciaNoDisponibleException(
                "cuentas-service respondió con error al procesar la cuenta " + cuentaId, ex);
    }

    private CuentaResumen rethrowOrDependency(String accion, Long cuentaId, Throwable throwable) {
        if (throwable instanceof RecursoNoEncontradoException ex) {
            throw ex;
        }
        if (throwable instanceof OperacionPagoException ex) {
            throw ex;
        }
        if (throwable instanceof DependenciaNoDisponibleException ex) {
            throw ex;
        }
        throw dependencia(accion, cuentaId, throwable);
    }

    private void relay(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private DependenciaNoDisponibleException dependencia(String accion, Long cuentaId, Throwable throwable) {
        return new DependenciaNoDisponibleException(
                "cuentas-service no disponible al " + accion + " la cuenta " + cuentaId
                        + ". Circuit Breaker/Fallback activo.",
                throwable);
    }
}
