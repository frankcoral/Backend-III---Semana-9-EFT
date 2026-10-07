package com.bancoxyz.clientes.repository;

import com.bancoxyz.clientes.model.Cliente;
import com.bancoxyz.clientes.model.EstadoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByRut(String rut);

    boolean existsByRut(String rut);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Cliente> findByEstadoOrderByApellidosAscNombresAsc(EstadoCliente estado);
}
