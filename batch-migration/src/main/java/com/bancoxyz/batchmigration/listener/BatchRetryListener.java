package com.bancoxyz.batchmigration.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.dao.TransientDataAccessException;

@Component
public class BatchRetryListener implements RetryListener {

    private static final Logger log = LoggerFactory.getLogger(BatchRetryListener.class);

    @Override
    public void beforeRetry(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            RetryState retryState) {

        log.warn(
                "RETRY -> nuevoIntento={}, ultimoError={}, hilo={}",
                retryState.getRetryCount() + 1,
                retryState.getLastException() == null
                        ? "SIN_ERROR"
                        : retryState.getLastException().getMessage(),
                Thread.currentThread().getName());
    }

    @Override
    public void onRetryFailure(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            Throwable throwable) {

        log.warn(
                "RETRY FALLIDO -> tipo={}, motivo={}, hilo={}",
                throwable.getClass().getSimpleName(),
                throwable.getMessage(),
                Thread.currentThread().getName());
    }

    @Override
    public void onRetryPolicyExhaustion(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            RetryException exception) {

        Throwable causa = exception.getCause();

        if (causa instanceof TransientDataAccessException) {
            log.error(
                    "RETRY AGOTADO -> reintentos={}, causa={}, hilo={}",
                    exception.getRetryCount(),
                    causa.getMessage(),
                    Thread.currentThread().getName());
        } else {
            /*
             * Los errores de calidad de datos no son reintentables. Se delegan
             * a SkipPolicy/SkipListener y no deben parecer fallos de retry.
             */
            log.debug(
                    "SIN RETRY -> excepcion no reintentable delegada a tolerancia de datos: {}, hilo={}",
                    causa == null ? exception.getMessage() : causa.getMessage(),
                    Thread.currentThread().getName());
        }
    }
}
