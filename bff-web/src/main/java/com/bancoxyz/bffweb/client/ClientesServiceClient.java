package com.bancoxyz.bffweb.client;

import com.bancoxyz.bffweb.dto.WebClienteDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClientesServiceClient {

    private final RestClient restClient;

    public ClientesServiceClient(
            @Qualifier("clientesServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public WebClienteDto obtenerPorRut(String rut, String accessToken) {
        return restClient.get()
                .uri("/api/clientes/rut/{rut}", rut)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(WebClienteDto.class);
    }
}
