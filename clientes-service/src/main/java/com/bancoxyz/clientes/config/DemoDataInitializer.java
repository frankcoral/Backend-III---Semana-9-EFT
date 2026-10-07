package com.bancoxyz.clientes.config;

import com.bancoxyz.clientes.model.Cliente;
import com.bancoxyz.clientes.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    public static final String DEMO_RUT = "11111111-1";
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private final ClienteRepository repository;

    public DemoDataInitializer(ClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.findByRut(DEMO_RUT).isPresent()) {
            return;
        }
        Cliente demo = repository.save(new Cliente(
                DEMO_RUT,
                "Cliente",
                "Demo EFT",
                "demo@bancoxyz.cl",
                "+56 9 1111 1111",
                "Sucursal virtual Banco XYZ"));
        log.info("DEMO_DATA_CLIENTE -> id={}, rut={}", demo.getId(), demo.getRut());
    }
}
