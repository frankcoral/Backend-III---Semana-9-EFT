package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.client.CuentaClient;
import com.bancoxyz.pagos.dto.CuentaResumen;
import com.bancoxyz.pagos.dto.DepositoRequest;
import com.bancoxyz.pagos.messaging.TransaccionEventProducer;
import com.bancoxyz.pagos.model.Transaccion;
import com.bancoxyz.pagos.repository.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock TransaccionRepository repository;
    @Mock CuentaClient cuentaClient;
    @Mock TransaccionEventProducer eventProducer;

    private PagoService service;

    @BeforeEach
    void setUp() {
        service = new PagoService(repository, cuentaClient, eventProducer);
        when(repository.saveAndFlush(any(Transaccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventProducer.publicar(any())).thenReturn(true);
    }

    @Test
    void depositoActualizaSaldoYPublicaEvento() {
        CuentaResumen cuenta = new CuentaResumen(101L, "CTA-101", 1L, "AHORRO", "ACTIVA",
                new BigDecimal("8000.00"), "CLP");
        when(cuentaClient.obtener(eq(101L), anyString())).thenReturn(cuenta);
        when(cuentaClient.actualizarSaldo(eq(101L), eq(new BigDecimal("9000.00")), anyString()))
                .thenReturn(new CuentaResumen(101L, "CTA-101", 1L, "AHORRO", "ACTIVA",
                        new BigDecimal("9000.00"), "CLP"));

        var response = service.depositar(new DepositoRequest(101L, new BigDecimal("1000.00"), "Deposito test"), "Bearer test");

        assertEquals("APROBADA", response.estado().name());
        assertTrue(response.eventoPublicado());
        verify(cuentaClient).actualizarSaldo(101L, new BigDecimal("9000.00"), "Bearer test");
        verify(eventProducer).publicar(any());
    }
}
