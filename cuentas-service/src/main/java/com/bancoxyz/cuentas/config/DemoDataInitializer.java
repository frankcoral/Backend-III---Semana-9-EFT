package com.bancoxyz.cuentas.config;

import com.bancoxyz.cuentas.model.Cuenta;
import com.bancoxyz.cuentas.model.TipoCuenta;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String DEMO_ACCOUNT = "CTA-DEMO-000001";
    private final CuentaRepository repository;

    public DemoDataInitializer(CuentaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.findByNumeroCuenta(DEMO_ACCOUNT).isPresent()) {
            return;
        }
        Cuenta demo = repository.save(new Cuenta(
                DEMO_ACCOUNT,
                1L,
                TipoCuenta.AHORRO,
                new BigDecimal("100000.00"),
                "CLP"));
        log.info("DEMO_DATA_CUENTA -> id={}, numero={}, clienteId={}, saldo={}",
                demo.getId(), demo.getNumeroCuenta(), demo.getClienteId(), demo.getSaldo());
    }
}
