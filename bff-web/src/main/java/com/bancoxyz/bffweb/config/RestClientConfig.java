package com.bancoxyz.bffweb.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("clientesServiceRestClient")
    RestClient clientesServiceRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
        return builder.clone().baseUrl("http://clientes-service").build();
    }

    @Bean("cuentasServiceRestClient")
    RestClient cuentasServiceRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
        return builder.clone().baseUrl("http://cuentas-service").build();
    }

    @Bean("pagosServiceRestClient")
    RestClient pagosServiceRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
        return builder.clone().baseUrl("http://pagos-service").build();
    }
}
