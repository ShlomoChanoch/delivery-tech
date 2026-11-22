package com.deliverytech.delivery_api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliverytech.delivery_api.dto.request.LoginRequest;
import com.deliverytech.delivery_api.dto.request.RegisterRequest;
import com.deliverytech.delivery_api.model.Role;
import com.deliverytech.delivery_api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo real: register 201 → login 200 com JWT → rota protegida 200")
    void registerLoginEAcessoComJwtReal_DeveFuncionar() throws Exception {
        RegisterRequest register =
                new RegisterRequest("auth.it@teste.com", "SenhaForte123!", "Usuário IT", Role.USER);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername("auth.it@teste.com");
        login.setPassword("SenhaForte123!");

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").isNotEmpty())
                        .andExpect(jsonPath("$.username").value("auth.it@teste.com"))
                        .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/login - senha errada deve retornar 401")
    void loginComSenhaIncorreta_DeveRetornar401() throws Exception {
        RegisterRequest register =
                new RegisterRequest("senha.it@teste.com", "SenhaCorreta123!", "Usuário", Role.USER);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest loginErrado = new LoginRequest();
        loginErrado.setUsername("senha.it@teste.com");
        loginErrado.setPassword("SenhaErrada");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginErrado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rota protegida sem token ou com token inválido/malformado deve retornar 403")
    void rotaProtegidaSemTokenOuTokenInvalido_DeveRetornar403() throws Exception {
        mockMvc.perform(get("/api/clientes")).andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/clientes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token.malformado.aqui"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token JWT válido de usuário removido do banco deve retornar 403 (não 500)")
    void tokenDeUsuarioDeletado_DeveRetornar403() throws Exception {
        RegisterRequest register =
                new RegisterRequest("temp.it@teste.com", "Senha123!", "Temp", Role.USER);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername("temp.it@teste.com");
        login.setPassword("Senha123!");

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andReturn();

        String token =
                objectMapper
                        .readTree(loginResult.getResponse().getContentAsString())
                        .get("token")
                        .asText();

        usuarioRepository.deleteAll();

        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
