package com.bancoxyz.clientes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientesServiceApplicationTests {
    @Test
    void clasePrincipalDisponible() {
        assertNotNull(ClientesServiceApplication.class);
    }
}
