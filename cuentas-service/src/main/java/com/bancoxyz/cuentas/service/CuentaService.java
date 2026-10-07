package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.dto.*;
import com.bancoxyz.cuentas.event.CuentaEvent;
import com.bancoxyz.cuentas.exception.OperacionCuentaException;
import com.bancoxyz.cuentas.exception.RecursoNoEncontradoException;
import com.bancoxyz.cuentas.messaging.CuentaEventProducer;
import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.model.EstadoCuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CuentaService {

    private final CuentaRepository repository;
    private final CuentaEventProducer eventProducer;

    public CuentaService(CuentaRepository repository, CuentaEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public CuentaResponse crear(CrearCuentaRequest request) {
        Cuenta cuenta = new Cuenta(
                generarNumeroCuenta(),
                request.clienteId(),
                request.tipo(),
                request.saldoInicial(),
                request.moneda().toUpperCase());
        Cuenta guardada = repository.saveAndFlush(cuenta);
        publicar("CUENTA_CREADA", guardada);
        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public CuentaResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public CuentaResponse obtenerPorNumero(String numeroCuenta) {
        return repository.findByNumeroCuenta(numeroCuenta)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada: " + numeroCuenta));
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> listarPorCliente(Long clienteId) {
        return repository.findByClienteIdOrderByIdAsc(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CuentaResponse actualizar(Long id, ActualizarCuentaRequest request) {
        Cuenta cuenta = buscar(id);
        validarActiva(cuenta);
        cuenta.actualizarDatos(request.tipo(), request.moneda());
        Cuenta guardada = repository.saveAndFlush(cuenta);
        publicar("CUENTA_ACTUALIZADA", guardada);
        return toResponse(guardada);
    }

    @Transactional
    public CuentaResponse actualizarSaldo(Long id, ActualizarSaldoRequest request) {
        Cuenta cuenta = buscar(id);
        validarActiva(cuenta);
        cuenta.actualizarSaldo(request.nuevoSaldo());
        Cuenta guardada = repository.saveAndFlush(cuenta);
        publicar("SALDO_ACTUALIZADO", guardada);
        return toResponse(guardada);
    }

    @Transactional
    public CuentaResponse cerrar(Long id) {
        Cuenta cuenta = buscar(id);
        validarActiva(cuenta);
        if (cuenta.getSaldo().signum() != 0) {
            throw new OperacionCuentaException("La cuenta solo puede cerrarse con saldo 0");
        }
        cuenta.cerrar();
        Cuenta guardada = repository.saveAndFlush(cuenta);
        publicar("CUENTA_CERRADA", guardada);
        return toResponse(guardada);
    }

    private Cuenta buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada: " + id));
    }

    private void validarActiva(Cuenta cuenta) {
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new OperacionCuentaException("La cuenta no esta activa");
        }
    }

    private String generarNumeroCuenta() {
        String numero;
        do {
            numero = "CTA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (repository.existsByNumeroCuenta(numero));
        return numero;
    }

    private void publicar(String tipoEvento, Cuenta cuenta) {
        eventProducer.publicar(new CuentaEvent(
                UUID.randomUUID(),
                tipoEvento,
                cuenta.getId(),
                cuenta.getNumeroCuenta(),
                cuenta.getClienteId(),
                cuenta.getSaldo(),
                cuenta.getEstado().name(),
                Instant.now()));
    }

    private CuentaResponse toResponse(Cuenta c) {
        return new CuentaResponse(
                c.getId(), c.getNumeroCuenta(), c.getClienteId(), c.getTipo(), c.getEstado(),
                c.getSaldo(), c.getMoneda(), c.getCreadaEn(), c.getActualizadaEn());
    }
}
