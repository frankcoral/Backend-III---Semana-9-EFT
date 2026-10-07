package com.bancoxyz.cuentas.repository;

import com.bancoxyz.cuentas.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteIdOrderByIdAsc(Long clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);
}
