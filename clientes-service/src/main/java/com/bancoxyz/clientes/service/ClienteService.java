package com.bancoxyz.clientes.service;

import com.bancoxyz.clientes.dto.*;
import com.bancoxyz.clientes.event.ClienteEvent;
import com.bancoxyz.clientes.exception.OperacionClienteException;
import com.bancoxyz.clientes.exception.RecursoNoEncontradoException;
import com.bancoxyz.clientes.messaging.ClienteEventProducer;
import com.bancoxyz.clientes.model.Cliente;
import com.bancoxyz.clientes.model.EstadoCliente;
import com.bancoxyz.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository repository;
    private final ClienteEventProducer eventProducer;

    public ClienteService(ClienteRepository repository, ClienteEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    public ClienteResponse crear(CrearClienteRequest request) {
        String rut = normalizarRut(request.rut());
        String email = request.email().trim().toLowerCase();
        if (repository.existsByRut(rut))
            throw new OperacionClienteException("Ya existe un cliente con el RUT indicado");
        if (repository.existsByEmail(email))
            throw new OperacionClienteException("Ya existe un cliente con el correo indicado");

        Cliente cliente = repository.save(new Cliente(
                rut, request.nombres().trim(), request.apellidos().trim(), email,
                limpiar(request.telefono()), limpiar(request.direccion())));
        publicar("CLIENTE_CREADO", cliente);
        return map(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return map(buscar(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorRut(String rut) {
        Cliente cliente = repository.findByRut(normalizarRut(rut))
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado para RUT " + rut));
        return map(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(EstadoCliente estado) {
        List<Cliente> clientes = estado == null ? repository.findAll()
                : repository.findByEstadoOrderByApellidosAscNombresAsc(estado);
        return clientes.stream().map(this::map).toList();
    }

    public ClienteResponse actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = buscar(id);
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new OperacionClienteException("El correo ya esta asociado a otro cliente");
        }
        cliente.actualizarPerfil(request.nombres().trim(), request.apellidos().trim(), email,
                limpiar(request.telefono()), limpiar(request.direccion()));
        Cliente guardado = repository.save(cliente);
        publicar("CLIENTE_ACTUALIZADO", guardado);
        return map(guardado);
    }

    public ClienteResponse desactivar(Long id) {
        Cliente cliente = buscar(id);
        cliente.desactivar();
        Cliente guardado = repository.save(cliente);
        publicar("CLIENTE_DESACTIVADO", guardado);
        return map(guardado);
    }

    public ClienteResponse activar(Long id) {
        Cliente cliente = buscar(id);
        cliente.activar();
        Cliente guardado = repository.save(cliente);
        publicar("CLIENTE_ACTIVADO", guardado);
        return map(guardado);
    }

    private Cliente buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private void publicar(String tipo, Cliente cliente) {
        eventProducer.publicar(
                ClienteEvent.crear(tipo, cliente.getId(), cliente.getRut(), cliente.getEmail(), cliente.getEstado()));
    }

    private ClienteResponse map(Cliente c) {
        return new ClienteResponse(c.getId(), c.getRut(), c.getNombres(), c.getApellidos(), c.getEmail(),
                c.getTelefono(), c.getDireccion(), c.getEstado(), c.getCreadoEn(), c.getActualizadoEn());
    }

    private String normalizarRut(String rut) {
        return rut.trim().toUpperCase().replace(".", "").replace(" ", "");
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
