package com.deliverytech.delivery_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve permitir até o limite e bloquear com 429 + Retry-After ao exceder")
    void devePermitirAteLimiteEBloquearCom429() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthRateLimitFilter filter =
                new AuthRateLimitFilter(true, "/api/auth/", 2, 60, objectMapper, registry);

        for (int i = 1; i <= 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, res, chain);

            assertEquals(200, res.getStatus());
            assertNotNull(chain.getRequest());
            assertEquals("2", res.getHeader("X-RateLimit-Limit"));
            assertEquals(String.valueOf(2 - i), res.getHeader("X-RateLimit-Remaining"));
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedReq, blockedRes, blockedChain);

        assertEquals(429, blockedRes.getStatus());
        assertNull(blockedChain.getRequest());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Muitas requisições"));
        assertEquals(
                1.0,
                registry.get("delivery.rate_limit.rejected")
                        .tag("endpoint", "auth")
                        .counter()
                        .count());
    }

    @Test
    @DisplayName("Rotas fora de /api/auth/** não sofrem rate limit")
    void rotasForaDeAuth_NaoSofremRateLimit() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthRateLimitFilter filter =
                new AuthRateLimitFilter(true, "/api/auth/", 1, 60, objectMapper, registry);

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/painel");
            req.setRemoteAddr("10.0.0.2");
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, res, chain);

            assertEquals(200, res.getStatus());
            assertNotNull(chain.getRequest());
            assertNull(res.getHeader("X-RateLimit-Limit"));
        }
    }

    @Test
    @DisplayName("Quando desabilitado, permite requisições ilimitadas em /api/auth/**")
    void quandoDesabilitado_PermiteTodasAsRequisicoes() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthRateLimitFilter filter =
                new AuthRateLimitFilter(false, "/api/auth/", 1, 60, objectMapper, registry);

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.3");
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, res, chain);

            assertEquals(200, res.getStatus());
            assertNotNull(chain.getRequest());
        }
    }

    @Test
    @DisplayName("X-Forwarded-For isola buckets por IP de origem")
    void xForwardedFor_IsolaBucketsPorIp() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthRateLimitFilter filter =
                new AuthRateLimitFilter(true, "/api/auth/", 1, 60, objectMapper, registry);

        MockHttpServletRequest reqIpA = new MockHttpServletRequest("POST", "/api/auth/login");
        reqIpA.addHeader("X-Forwarded-For", "198.51.100.10, 10.0.0.1");
        MockHttpServletResponse resIpA = new MockHttpServletResponse();
        filter.doFilter(reqIpA, resIpA, new MockFilterChain());
        assertEquals(200, resIpA.getStatus());

        MockHttpServletRequest reqIpB = new MockHttpServletRequest("POST", "/api/auth/login");
        reqIpB.addHeader("X-Forwarded-For", "198.51.100.20");
        MockHttpServletResponse resIpB = new MockHttpServletResponse();
        filter.doFilter(reqIpB, resIpB, new MockFilterChain());
        assertEquals(200, resIpB.getStatus());
    }
}
