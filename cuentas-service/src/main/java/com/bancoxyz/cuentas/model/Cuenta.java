package com.bancoxyz.cuentas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cuentas", indexes = {
        @Index(name = "idx_cuenta_cliente", columnList = "cliente_id"),
        @Index(name = "idx_cuenta_numero", columnList = "numero_cuenta", unique = true)
})
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 32)
    private String numeroCuenta;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCuenta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    /**
     * Control optimista para evitar que dos operaciones concurrentes sobrescriban
     * silenciosamente el saldo de la misma cuenta.
     */
    @Version
    private Long version;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    protected Cuenta() {
    }

    public Cuenta(String numeroCuenta, Long clienteId, TipoCuenta tipo, BigDecimal saldo, String moneda) {
        this.numeroCuenta = numeroCuenta;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.saldo = saldo;
        this.moneda = moneda;
        this.estado = EstadoCuenta.ACTIVA;
        this.creadaEn = Instant.now();
        this.actualizadaEn = this.creadaEn;
    }

    @PreUpdate
    void preUpdate() {
        this.actualizadaEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public TipoCuenta getTipo() {
        return tipo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public String getMoneda() {
        return moneda;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public void actualizarDatos(TipoCuenta tipo, String moneda) {
        if (tipo != null)
            this.tipo = tipo;
        if (moneda != null && !moneda.isBlank())
            this.moneda = moneda.toUpperCase();
        this.actualizadaEn = Instant.now();
    }

    public void actualizarSaldo(BigDecimal nuevoSaldo) {
        this.saldo = nuevoSaldo;
        this.actualizadaEn = Instant.now();
    }

    public void cerrar() {
        this.estado = EstadoCuenta.CERRADA;
        this.actualizadaEn = Instant.now();
    }
}
