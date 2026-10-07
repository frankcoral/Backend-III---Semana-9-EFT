package com.bancoxyz.pagos.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Builder normal para clientes internos de infraestructura (por ejemplo
     * Eureka).
     * Debe ser el bean primario para evitar que Eureka use por accidente
     * el RestClient configurado con Spring Cloud LoadBalancer.
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Builder exclusivo para llamadas de negocio mediante nombre lógico de Eureka,
     * por ejemplo http://cuentas-service.
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
