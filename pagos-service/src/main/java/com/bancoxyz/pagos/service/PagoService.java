package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.client.CuentaClient;
import com.bancoxyz.pagos.dto.*;
import com.bancoxyz.pagos.event.TransaccionEvent;
import com.bancoxyz.pagos.exception.ConsistenciaDistribuidaException;
import com.bancoxyz.pagos.exception.OperacionPagoException;
import com.bancoxyz.pagos.exception.RecursoNoEncontradoException;
import com.bancoxyz.pagos.messaging.TransaccionEventProducer;
import com.bancoxyz.pagos.model.EstadoTransaccion;
import com.bancoxyz.pagos.model.TipoTransaccion;
import com.bancoxyz.pagos.model.Transaccion;
import com.bancoxyz.pagos.repository.TransaccionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PagoService {

    private final TransaccionRepository repository;
    private final CuentaClient cuentaClient;
    private final TransaccionEventProducer eventProducer;

    public PagoService(TransaccionRepository repository,
            CuentaClient cuentaClient,
            TransaccionEventProducer eventProducer) {
        this.repository = repository;
        this.cuentaClient = cuentaClient;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public TransaccionResponse depositar(DepositoRequest request, String authorization) {
        CuentaResumen destino = cuentaActiva(cuentaClient.obtener(request.cuentaId(), authorization));
        Transaccion tx = guardarPendiente(TipoTransaccion.DEPOSITO, null, destino.id(),
                request.monto(), null, request.descripcion());

        cuentaClient.actualizarSaldo(destino.id(), destino.saldo().add(request.monto()), authorization);
        aprobarYPublicar(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransaccionResponse pagar(PagoRequest request, String authorization) {
        CuentaResumen origen = cuentaActiva(cuentaClient.obtener(request.cuentaId(), authorization));
        Transaccion tx = guardarPendiente(TipoTransaccion.PAGO, origen.id(), null,
                request.monto(), request.beneficiario(), request.descripcion());

        validarFondos(origen, request.monto(), tx);
        cuentaClient.actualizarSaldo(origen.id(), origen.saldo().subtract(request.monto()), authorization);
        aprobarYPublicar(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransaccionResponse transferir(TransferenciaRequest request, String authorization) {
        if (request.cuentaOrigenId().equals(request.cuentaDestinoId())) {
            throw new OperacionPagoException("La cuenta de origen y destino deben ser diferentes");
        }

        CuentaResumen origen = cuentaActiva(cuentaClient.obtener(request.cuentaOrigenId(), authorization));
        CuentaResumen destino = cuentaActiva(cuentaClient.obtener(request.cuentaDestinoId(), authorization));
        if (!origen.moneda().equalsIgnoreCase(destino.moneda())) {
            throw new OperacionPagoException("Las cuentas deben utilizar la misma moneda");
        }

        Transaccion tx = guardarPendiente(TipoTransaccion.TRANSFERENCIA, origen.id(), destino.id(),
                request.monto(), null, request.descripcion());
        validarFondos(origen, request.monto(), tx);

        BigDecimal saldoOrigenOriginal = origen.saldo();
        cuentaClient.actualizarSaldo(origen.id(), saldoOrigenOriginal.subtract(request.monto()), authorization);
        try {
            cuentaClient.actualizarSaldo(destino.id(), destino.saldo().add(request.monto()), authorization);
        } catch (RuntimeException ex) {
            compensarOrigen(origen.id(), saldoOrigenOriginal, authorization, ex);
            tx.rechazar("Transferencia revertida por error al acreditar la cuenta destino");
            repository.saveAndFlush(tx);
            throw ex;
        }

        aprobarYPublicar(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransaccionResponse retirar(RetiroRequest request, String authorization) {
        CuentaResumen origen = cuentaActiva(cuentaClient.obtener(request.cuentaId(), authorization));
        Transaccion tx = guardarPendiente(TipoTransaccion.RETIRO, origen.id(), null,
                request.monto(), null, request.descripcion());

        validarFondos(origen, request.monto(), tx);
        cuentaClient.actualizarSaldo(origen.id(), origen.saldo().subtract(request.monto()), authorization);
        aprobarYPublicar(tx);
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public TransaccionResponse obtener(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Transaccion no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarPorCuenta(Long cuentaId, int limit) {
        int limiteSeguro = Math.max(1, Math.min(limit, 100));
        return repository.findByCuentaOrigenIdOrCuentaDestinoIdOrderByCreadaEnDesc(
                        cuentaId, cuentaId, PageRequest.of(0, limiteSeguro))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public boolean reintentarEvento(Transaccion tx) {
        if (tx.getEstado() != EstadoTransaccion.APROBADA || tx.isEventoPublicado()) {
            return false;
        }
        boolean publicado = eventProducer.publicar(toEvent(tx));
        if (publicado) {
            tx.marcarEventoPublicado();
            repository.save(tx);
        }
        return publicado;
    }

    private Transaccion guardarPendiente(TipoTransaccion tipo, Long origen, Long destino,
            BigDecimal monto, String beneficiario, String descripcion) {
        return repository.saveAndFlush(new Transaccion(tipo, origen, destino, monto, beneficiario, descripcion));
    }

    private CuentaResumen cuentaActiva(CuentaResumen cuenta) {
        if (cuenta == null) {
            throw new OperacionPagoException("cuentas-service devolvio una respuesta vacia");
        }
        if (!"ACTIVA".equalsIgnoreCase(cuenta.estado())) {
            throw new OperacionPagoException("La cuenta " + cuenta.id() + " no esta activa");
        }
        return cuenta;
    }

    private void validarFondos(CuentaResumen cuenta, BigDecimal monto, Transaccion tx) {
        if (cuenta.saldo().compareTo(monto) < 0) {
            tx.rechazar("SALDO_INSUFICIENTE");
            repository.saveAndFlush(tx);
            throw new OperacionPagoException("Saldo insuficiente en la cuenta " + cuenta.id());
        }
    }

    private void compensarOrigen(Long cuentaId, BigDecimal saldoOriginal, String authorization,
            RuntimeException causa) {
        try {
            cuentaClient.actualizarSaldo(cuentaId, saldoOriginal, authorization);
        } catch (RuntimeException compensacionError) {
            causa.addSuppressed(compensacionError);
            throw new ConsistenciaDistribuidaException(
                    "Fallo critico de consistencia: no fue posible compensar la cuenta origen " + cuentaId,
                    causa);
        }
    }

    private void aprobarYPublicar(Transaccion tx) {
        tx.aprobar();
        repository.saveAndFlush(tx);
        if (eventProducer.publicar(toEvent(tx))) {
            tx.marcarEventoPublicado();
            repository.saveAndFlush(tx);
        }
    }

    private TransaccionEvent toEvent(Transaccion tx) {
        return new TransaccionEvent(
                UUID.randomUUID(), "TRANSACCION_COMPLETADA", tx.getId(), tx.getReferencia(),
                tx.getTipo(), tx.getEstado(), tx.getCuentaOrigenId(), tx.getCuentaDestinoId(),
                tx.getMonto(), Instant.now());
    }

    private TransaccionResponse toResponse(Transaccion tx) {
        return new TransaccionResponse(
                tx.getId(), tx.getReferencia(), tx.getTipo(), tx.getEstado(),
                tx.getCuentaOrigenId(), tx.getCuentaDestinoId(), tx.getMonto(), tx.getBeneficiario(),
                tx.getDescripcion(), tx.getCreadaEn(), tx.getProcesadaEn(), tx.isEventoPublicado(),
                tx.getDetalleError());
    }
}
