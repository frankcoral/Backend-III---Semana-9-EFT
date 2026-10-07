package com.bancoxyz.bffatm.client;

import com.bancoxyz.bffatm.dto.CuentaAtmDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CuentasServiceClient {

    private final RestClient restClient;

    public CuentasServiceClient(@Qualifier("cuentasServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public CuentaAtmDto obtener(Long cuentaId, String accessToken) {
        return restClient.get()
                .uri("/api/cuentas/{cuentaId}", cuentaId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(CuentaAtmDto.class);
    }
}
