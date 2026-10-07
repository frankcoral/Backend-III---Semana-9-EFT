package com.bancoxyz.clientes.service;

import com.bancoxyz.clientes.dto.CrearClienteRequest;
import com.bancoxyz.clientes.messaging.ClienteEventProducer;
import com.bancoxyz.clientes.model.Cliente;
import com.bancoxyz.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock ClienteRepository repository;
    @Mock ClienteEventProducer eventProducer;
    @InjectMocks ClienteService service;

    @Test
    void creaClienteValido() {
        var request = new CrearClienteRequest("12.345.678-5", "Ana", "Perez", "ANA@MAIL.CL", "+56911111111", "Calama");
        when(repository.existsByRut("12345678-5")).thenReturn(false);
        when(repository.existsByEmail("ana@mail.cl")).thenReturn(false);
        when(repository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.crear(request);

        assertEquals("12345678-5", response.rut());
        assertEquals("ana@mail.cl", response.email());
        verify(repository).save(any(Cliente.class));
        verify(eventProducer).publicar(any());
    }
}
