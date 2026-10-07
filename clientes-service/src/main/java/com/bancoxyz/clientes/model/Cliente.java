package com.bancoxyz.clientes.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_rut", columnNames = "rut"),
        @UniqueConstraint(name = "uk_clientes_email", columnNames = "email")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String rut;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 250)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCliente estado;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Instant actualizadoEn;

    protected Cliente() {
    }

    public Cliente(String rut, String nombres, String apellidos, String email, String telefono, String direccion) {
        this.rut = rut;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.estado = EstadoCliente.ACTIVO;
    }

    @PrePersist
    void prePersist() {
        Instant ahora = Instant.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
        if (estado == null)
            estado = EstadoCliente.ACTIVO;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = Instant.now();
    }

    public void actualizarPerfil(String nombres, String apellidos, String email, String telefono, String direccion) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public void activar() {
        this.estado = EstadoCliente.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoCliente.INACTIVO;
    }

    public Long getId() {
        return id;
    }

    public String getRut() {
        return rut;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public EstadoCliente getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
