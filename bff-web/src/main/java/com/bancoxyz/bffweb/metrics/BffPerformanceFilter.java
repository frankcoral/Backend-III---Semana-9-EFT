package com.bancoxyz.bffweb.metrics;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class BffPerformanceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BffPerformanceFilter.class);
    private static final String CHANNEL = "WEB";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            int responseBytes = wrapper.getContentAsByteArray().length;
            log.info(
                    "BFF_PERFORMANCE -> canal={}, metodo={}, endpoint={}, status={}, duracionMs={}, responseBytes={}",
                    CHANNEL,
                    request.getMethod(),
                    request.getRequestURI(),
                    wrapper.getStatus(),
                    durationMs,
                    responseBytes);
            wrapper.copyBodyToResponse();
        }
    }
}
