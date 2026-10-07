package com.bancoxyz.pagos.config;

import com.bancoxyz.pagos.model.TipoTransaccion;
import com.bancoxyz.pagos.model.Transaccion;
import com.bancoxyz.pagos.repository.TransaccionRepository;
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
    private final TransaccionRepository repository;

    public DemoDataInitializer(TransaccionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        guardar(TipoTransaccion.DEPOSITO, null, 1L, "10000.00", "Abono de demostración");
        guardar(TipoTransaccion.PAGO, 1L, null, "5000.00", "Pago de servicio de demostración");
        guardar(TipoTransaccion.RETIRO, 1L, null, "5000.00", "Retiro ATM de demostración");
        log.info("DEMO_DATA_PAGOS -> 3 transacciones históricas creadas para cuentaId=1");
    }

    private void guardar(TipoTransaccion tipo, Long origen, Long destino, String monto, String descripcion) {
        Transaccion tx = new Transaccion(tipo, origen, destino, new BigDecimal(monto), null, descripcion);
        tx.aprobar();
        tx.marcarEventoPublicado();
        repository.save(tx);
    }
}
