package com.deliverytech.delivery_api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliverytech.delivery_api.dto.request.ClienteRequest;
import com.deliverytech.delivery_api.repository.ClienteRepository;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import com.deliverytech.delivery_api.service.ClienteService;
import com.deliverytech.delivery_api.service.RelatorioService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "spring.cache.type=caffeine",
            "spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=5m,recordStats",
            "app.rate-limit.auth.enabled=true",
            "app.rate-limit.auth.max-requests=3",
            "app.rate-limit.auth.window-seconds=60"
        })
class CacheAndRateLimitIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CacheManager cacheManager;
    @Autowired private RelatorioService relatorioService;
    @Autowired private ClienteService clienteService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ClienteRepository clienteRepository;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();
        clienteRepository.deleteAll();
        cacheManager
                .getCacheNames()
                .forEach(name -> {
                    Cache cache = cacheManager.getCache(name);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }

    @Test
    @DisplayName(
            "Caffeine deve cachear resumoVendas e invalidar automaticamente ao cadastrar cliente")
    void cacheDeRelatorio_DeveCachearEInvalidarNaEscrita() {
        Map<String, Object> primeiro = relatorioService.resumoVendas(null, null);
        assertEquals(0L, primeiro.get("clientesAtivos"));

        Cache cache = cacheManager.getCache(CacheConfig.RELATORIO_RESUMO_VENDAS);
        assertNotNull(cache);

        ClienteRequest novo = new ClienteRequest();
        novo.setNome("Cliente Cache");
        novo.setEmail("cache.it@teste.com");
        novo.setTelefone("11988887777");
        novo.setEndereco("Rua Cache, 10");
        clienteService.cadastrar(novo);

        Map<String, Object> aposEscrita = relatorioService.resumoVendas(null, null);
        assertEquals(1L, aposEscrita.get("clientesAtivos"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 4ª tentativa seguida pelo mesmo IP deve retornar 429")
    void rateLimitEmAuth_DeveRetornar429AoExcederLimite() throws Exception {
        String payload = "{\"username\":\"naoexiste@teste.com\",\"password\":\"errada\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .header("X-Forwarded-For", "203.0.113.42")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-RateLimit-Limit", "3"));
        }

        mockMvc.perform(
                        post("/api/auth/login")
                                .header("X-Forwarded-For", "203.0.113.42")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Muitas requisições"));
    }
}
