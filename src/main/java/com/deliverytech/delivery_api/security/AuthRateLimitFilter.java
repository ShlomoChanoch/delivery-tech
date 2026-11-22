package com.deliverytech.delivery_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiter por IP em memória (Caffeine) para proteger {@code /api/auth/**} contra força bruta.
 * Configurável via {@code app.rate-limit.auth.*} e instrumentado via Micrometer.
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final String pathPrefix;
    private final int maxRequests;
    private final long windowSeconds;
    private final ObjectMapper objectMapper;
    private final Cache<String, AtomicInteger> buckets;
    private final Counter rejectedCounter;

    public AuthRateLimitFilter(
            @Value("${app.rate-limit.auth.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth.path-prefix:/api/auth/}") String pathPrefix,
            @Value("${app.rate-limit.auth.max-requests:20}") int maxRequests,
            @Value("${app.rate-limit.auth.window-seconds:60}") long windowSeconds,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.enabled = enabled;
        this.pathPrefix = pathPrefix;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.objectMapper = objectMapper;
        this.buckets =
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofSeconds(windowSeconds))
                        .build();
        this.rejectedCounter =
                Counter.builder("delivery.rate_limit.rejected")
                        .description("Requisições rejeitadas por rate limit em /api/auth/**")
                        .tag("endpoint", "auth")
                        .register(meterRegistry);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(pathPrefix);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = resolveClientIp(request);
        AtomicInteger counter = buckets.get(clientKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader(
                "X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - current)));

        if (current > maxRequests) {
            rejectedCounter.increment();
            log.warn(
                    "Rate limit excedido em {} para IP {} ({}/{})",
                    request.getRequestURI(),
                    clientKey,
                    current,
                    maxRequests);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now(ZoneId.systemDefault()).toString());
            body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            body.put("error", "Muitas requisições");
            body.put("message", "Limite de tentativas excedido. Tente novamente em instantes.");
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
