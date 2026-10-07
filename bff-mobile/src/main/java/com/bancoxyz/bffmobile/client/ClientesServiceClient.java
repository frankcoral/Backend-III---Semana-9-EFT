package com.bancoxyz.bffmobile.client;

import com.bancoxyz.bffmobile.dto.MobileClienteDto;
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

    public MobileClienteDto obtenerPorRut(String rut, String accessToken) {
        return restClient.get()
                .uri("/api/clientes/rut/{rut}", rut)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(MobileClienteDto.class);
    }
}
