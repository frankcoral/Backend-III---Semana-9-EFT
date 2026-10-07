package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.dto.CrearCuentaRequest;
import com.bancoxyz.cuentas.messaging.CuentaEventProducer;
import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.model.TipoCuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock CuentaRepository repository;
    @Mock CuentaEventProducer producer;

    @Test
    void creaCuentaActivaYPublicaEvento() {
        when(repository.existsByNumeroCuenta(any())).thenReturn(false);
        when(repository.saveAndFlush(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaService service = new CuentaService(repository, producer);
        var response = service.crear(new CrearCuentaRequest(
                10L, TipoCuenta.AHORRO, new BigDecimal("5000.00"), "CLP"));

        assertEquals(10L, response.clienteId());
        assertEquals(new BigDecimal("5000.00"), response.saldo());
        assertEquals("CLP", response.moneda());
        verify(producer).publicar(any());
    }
}
