package com.deliverytech.delivery_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliverytech.delivery_api.dto.request.ProdutoRequest;
import com.deliverytech.delivery_api.model.Produto;
import com.deliverytech.delivery_api.model.Restaurante;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class ProdutoControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private RestauranteRepository restauranteRepository;

    private Restaurante restaurante;
    private Produto produto;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();

        restaurante =
                restauranteRepository.save(
                        Restaurante.builder()
                                .nome("Burger Lab")
                                .categoria("Hambúrguer")
                                .taxaEntrega(new BigDecimal("5.00"))
                                .ativo(true)
                                .build());

        produto =
                produtoRepository.save(
                        Produto.builder()
                                .nome("Smash Duplo")
                                .categoria("Hambúrguer")
                                .descricao("Dois blends com cheddar")
                                .preco(new BigDecimal("32.90"))
                                .disponivel(true)
                                .restaurante(restaurante)
                                .build());
    }

    @Test
    @DisplayName("POST /api/produtos - válido 201, restaurante inexistente 404, inválido 400")
    @WithMockUser
    void cadastrar_CenariosPrincipais() throws Exception {
        ProdutoRequest valido =
                new ProdutoRequest(
                        "Batata Rústica",
                        "Acompanhamento",
                        "Com páprica",
                        new BigDecimal("16.50"),
                        restaurante.getId());

        mockMvc.perform(
                        post("/api/produtos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(valido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Batata Rústica"))
                .andExpect(jsonPath("$.disponivel").value(true));

        ProdutoRequest restauranteInexistente =
                new ProdutoRequest(
                        "Milkshake", "Bebida", "Chocolate", new BigDecimal("19.00"), 99999L);

        mockMvc.perform(
                        post("/api/produtos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(restauranteInexistente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"));

        mockMvc.perform(
                        post("/api/produtos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ProdutoRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("GET /api/produtos, /{id}, /restaurante/{id}, /categoria/{cat} e /buscar")
    @WithMockUser
    void consultasDeProduto_DevemRetornar200E404() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/produtos/{id}", produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Smash Duplo"));

        mockMvc.perform(get("/api/produtos/{id}", 99999L)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/produtos/restaurante/{id}", restaurante.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Smash Duplo"));

        mockMvc.perform(get("/api/produtos/categoria/{categoria}", "Hambúrguer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/produtos/buscar").param("nome", "Smash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Smash Duplo"));
    }

    @Test
    @DisplayName("PUT /api/produtos/{id}, PATCH disponibilidade e DELETE /{id}")
    @WithMockUser
    void atualizarAlterarDisponibilidadeEDeletar() throws Exception {
        ProdutoRequest atualizado =
                new ProdutoRequest(
                        "Smash Triplo",
                        "Hambúrguer",
                        "Três blends",
                        new BigDecimal("39.90"),
                        restaurante.getId());

        mockMvc.perform(
                        put("/api/produtos/{id}", produto.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(atualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Smash Triplo"));

        mockMvc.perform(
                        patch("/api/produtos/{id}/disponibilidade", produto.getId())
                                .param("disponivel", "false"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/produtos/{id}", produto.getId()))
                .andExpect(status().isNoContent());
    }
}
