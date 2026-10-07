package com.bancoxyz.bffatm.client;

import com.bancoxyz.bffatm.dto.PagoRetiroRequestDto;
import com.bancoxyz.bffatm.dto.PagoRetiroResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class PagosServiceClient {

    private final RestClient restClient;

    public PagosServiceClient(@Qualifier("pagosServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public PagoRetiroResponseDto retirar(Long cuentaId, BigDecimal monto, String accessToken) {
        PagoRetiroRequestDto request = new PagoRetiroRequestDto(
                cuentaId,
                monto,
                "Retiro solicitado desde BFF ATM");

        return restClient.post()
                .uri("/api/pagos/retiros")
                .headers(h -> h.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PagoRetiroResponseDto.class);
    }
}
