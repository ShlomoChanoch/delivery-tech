package com.deliverytech.delivery_api.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class GlobalExceptionHandlerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /painel/nao-existe - deve retornar 404 com envelope padrão (não 500)")
    void pathInexistenteNoPainel_DeveRetornar404() throws Exception {
        mockMvc.perform(get("/painel/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /painel/ - barra final não mapeada deve retornar 404 com envelope padrão")
    void painelComBarraFinal_DeveRetornar404() throws Exception {
        mockMvc.perform(get("/painel/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /css/inexistente.css - asset estático inexistente deve retornar 404")
    void assetEstaticoInexistente_DeveRetornar404() throws Exception {
        mockMvc.perform(get("/css/inexistente.css"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("GET /api/rota-inexistente - rota autenticada inexistente deve retornar 404")
    @WithMockUser
    void rotaApiInexistenteAutenticada_DeveRetornar404() throws Exception {
        mockMvc.perform(get("/api/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
