package com.bancoxyz.pagos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transacciones", indexes = {
        @Index(name = "idx_transaccion_origen", columnList = "cuentaOrigenId"),
        @Index(name = "idx_transaccion_destino", columnList = "cuentaDestinoId"),
        @Index(name = "idx_transaccion_evento", columnList = "estado,eventoPublicado")
})
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransaccion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTransaccion estado;

    private Long cuentaOrigenId;
    private Long cuentaDestinoId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(length = 120)
    private String beneficiario;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, unique = true, length = 40)
    private String referencia;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    private Instant procesadaEn;

    @Column(nullable = false)
    private boolean eventoPublicado;

    @Column(length = 500)
    private String detalleError;

    protected Transaccion() {
    }

    public Transaccion(TipoTransaccion tipo, Long cuentaOrigenId, Long cuentaDestinoId,
            BigDecimal monto, String beneficiario, String descripcion) {
        this.tipo = tipo;
        this.estado = EstadoTransaccion.PENDIENTE;
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.beneficiario = beneficiario;
        this.descripcion = descripcion;
        this.referencia = "TRX-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
        this.creadaEn = Instant.now();
        this.eventoPublicado = false;
    }

    public void aprobar() {
        this.estado = EstadoTransaccion.APROBADA;
        this.procesadaEn = Instant.now();
        this.detalleError = null;
    }

    public void rechazar(String detalle) {
        this.estado = EstadoTransaccion.RECHAZADA;
        this.procesadaEn = Instant.now();
        this.detalleError = detalle;
    }

    public void marcarEventoPublicado() {
        this.eventoPublicado = true;
    }

    public UUID getId() {
        return id;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getBeneficiario() {
        return beneficiario;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getReferencia() {
        return referencia;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getProcesadaEn() {
        return procesadaEn;
    }

    public boolean isEventoPublicado() {
        return eventoPublicado;
    }

    public String getDetalleError() {
        return detalleError;
    }
}
