package com.bancoxyz.bffmobile.client;

import com.bancoxyz.bffmobile.dto.MobileTransaccionDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PagosServiceClient {

    private final RestClient restClient;

    public PagosServiceClient(
            @Qualifier("pagosServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<MobileTransaccionDto> listarPorCuenta(Long cuentaId, String accessToken) {
        return restClient.get()
                .uri(uri -> uri.path("/api/pagos")
                        .queryParam("cuentaId", cuentaId)
                        .queryParam("limit", 3)
                        .build())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
