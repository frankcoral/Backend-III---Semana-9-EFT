package com.bancoxyz.clientes.controller;

import com.bancoxyz.clientes.dto.ActualizarClienteRequest;
import com.bancoxyz.clientes.dto.ClienteResponse;
import com.bancoxyz.clientes.dto.CrearClienteRequest;
import com.bancoxyz.clientes.model.EstadoCliente;
import com.bancoxyz.clientes.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody CrearClienteRequest request) {
        return service.crear(request);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/rut/{rut}")
    public ClienteResponse obtenerPorRut(@PathVariable String rut) {
        return service.obtenerPorRut(rut);
    }

    @GetMapping
    public List<ClienteResponse> listar(@RequestParam(required = false) EstadoCliente estado) {
        return service.listar(estado);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarClienteRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public ClienteResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public ClienteResponse activar(@PathVariable Long id) {
        return service.activar(id);
    }
}
