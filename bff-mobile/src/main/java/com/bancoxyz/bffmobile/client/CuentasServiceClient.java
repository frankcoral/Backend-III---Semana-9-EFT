package com.bancoxyz.bffmobile.client;

import com.bancoxyz.bffmobile.dto.MobileCuentaDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CuentasServiceClient {

    private final RestClient restClient;

    public CuentasServiceClient(
            @Qualifier("cuentasServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<MobileCuentaDto> listarPorCliente(Long clienteId, String accessToken) {
        return restClient.get()
                .uri("/api/cuentas/cliente/{clienteId}", clienteId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
