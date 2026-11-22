package com.deliverytech.delivery_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliverytech.delivery_api.dto.request.RestauranteRequest;
import com.deliverytech.delivery_api.model.Produto;
import com.deliverytech.delivery_api.model.Restaurante;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class RestauranteControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private RestauranteRepository restauranteRepository;

    private Restaurante restaurante;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();

        restaurante =
                restauranteRepository.save(
                        Restaurante.builder()
                                .nome("Cantina Bella")
                                .categoria("Italiana")
                                .telefone("1133334444")
                                .taxaEntrega(new BigDecimal("6.50"))
                                .tempoEntregaMinutos(35)
                                .avaliacao(new BigDecimal("4.7"))
                                .ativo(true)
                                .build());
    }

    @Test
    @DisplayName("POST /api/restaurantes - deve cadastrar restaurante válido e retornar 201")
    @WithMockUser
    void cadastrar_ComDadosValidos_DeveRetornar201() throws Exception {
        RestauranteRequest request =
                new RestauranteRequest(
                        "Sushi Central",
                        "Japonesa",
                        "Av Paulista, 500",
                        new BigDecimal("8.00"),
                        40,
                        "1144445555",
                        "contato@sushi.com",
                        new BigDecimal("4.8"));

        mockMvc.perform(
                        post("/api/restaurantes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Sushi Central"))
                .andExpect(jsonPath("$.categoria").value("Japonesa"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("POST /api/restaurantes - deve retornar 400 para dados inválidos")
    @WithMockUser
    void cadastrar_ComDadosInvalidos_DeveRetornar400() throws Exception {
        RestauranteRequest invalido = new RestauranteRequest();

        mockMvc.perform(
                        post("/api/restaurantes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("GET /api/restaurantes - deve listar restaurantes e aceitar filtro por categoria")
    @WithMockUser
    void listarTodos_DeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/restaurantes").param("categoria", "Italiana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Cantina Bella"));
    }

    @Test
    @DisplayName("GET /api/restaurantes/{id} - existente 200 e inexistente 404")
    @WithMockUser
    void buscarPorId_ExistenteEInexistente() throws Exception {
        mockMvc.perform(get("/api/restaurantes/{id}", restaurante.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cantina Bella"));

        mockMvc.perform(get("/api/restaurantes/{id}", 99999L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/restaurantes/categoria/{categoria} e /disponiveis - devem retornar 200")
    @WithMockUser
    void buscarPorCategoriaEDisponiveis_DeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/restaurantes/categoria/{categoria}", "Italiana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Cantina Bella"));

        mockMvc.perform(get("/api/restaurantes/disponiveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/restaurantes/{id} - deve atualizar restaurante existente")
    @WithMockUser
    void atualizar_DeveRetornar200() throws Exception {
        RestauranteRequest request =
                new RestauranteRequest(
                        "Cantina Bella Premium",
                        "Italiana",
                        "Rua Augusta, 200",
                        new BigDecimal("7.50"),
                        30,
                        "1133334444",
                        "bella@teste.com",
                        new BigDecimal("4.9"));

        mockMvc.perform(
                        put("/api/restaurantes/{id}", restaurante.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cantina Bella Premium"));
    }

    @Test
    @DisplayName("PATCH /api/restaurantes/{id}/status e DELETE /{id} - devem alterar status")
    @WithMockUser
    void alterarStatusEInativar_DeveFuncionar() throws Exception {
        mockMvc.perform(
                        patch("/api/restaurantes/{id}/status", restaurante.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("ativo", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(delete("/api/restaurantes/{id}", restaurante.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/restaurantes/{id}/produtos - existente 200 e inexistente 404 JSON")
    @WithMockUser
    void buscarProdutosPorRestaurante_ExistenteEInexistente() throws Exception {
        produtoRepository.save(
                Produto.builder()
                        .nome("Lasanha Bolonhesa")
                        .categoria("Massas")
                        .descricao("Massa fresca")
                        .preco(new BigDecimal("42.00"))
                        .disponivel(true)
                        .restaurante(restaurante)
                        .build());

        mockMvc.perform(get("/api/restaurantes/{id}/produtos", restaurante.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Lasanha Bolonhesa"));

        mockMvc.perform(get("/api/restaurantes/{id}/produtos", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("POST /api/restaurantes/{id}/taxa-entrega - CEP válido 200 e CEP vazio 400")
    @WithMockUser
    void calcularTaxaEntrega_ValidoEInvalido() throws Exception {
        mockMvc.perform(
                        post("/api/restaurantes/{id}/taxa-entrega", restaurante.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("cep", "01310-100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restauranteId").value(restaurante.getId()))
                .andExpect(jsonPath("$.taxaEntrega").exists())
                .andExpect(jsonPath("$.moeda").value("BRL"));

        mockMvc.perform(
                        post("/api/restaurantes/{id}/taxa-entrega", restaurante.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("cep", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }
}
