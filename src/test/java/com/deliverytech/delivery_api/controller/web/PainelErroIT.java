package com.deliverytech.delivery_api.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliverytech.delivery_api.service.RelatorioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class PainelErroIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RelatorioService relatorioService;

    @Test
    @DisplayName("GET /painel/nao-existe com Accept HTML deve renderizar página de erro 404")
    void rotaInexistenteNoPainelHtml_DeveRenderizarErro404() throws Exception {
        mockMvc.perform(get("/painel/nao-existe").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Página não encontrada")))
                .andExpect(content().string(containsString("Voltar ao dashboard")))
                .andExpect(content().string(containsString("/painel/nao-existe")));
    }

    @Test
    @DisplayName("GET /painel/ com Accept HTML deve renderizar página de erro 404")
    void painelComBarraFinalHtml_DeveRenderizarErro404() throws Exception {
        mockMvc.perform(get("/painel/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Página não encontrada")));
    }

    @Test
    @DisplayName("Erro interno no PainelController deve renderizar painel/erro 500 sem vazar causa")
    void erroInternoNoPainel_DeveRenderizarErro500Amigavel() throws Exception {
        when(relatorioService.resumoVendas(null, null))
                .thenThrow(new RuntimeException("Falha interna simulada de banco"));

        mockMvc.perform(get("/painel").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Erro no painel")))
                .andExpect(content().string(containsString("Voltar ao dashboard")))
                .andExpect(content().string(not(containsString("Falha interna simulada"))));
    }

    @Test
    @DisplayName("GET /api/rota-inexistente mesmo com Accept HTML deve manter envelope JSON 404")
    @WithMockUser
    void rotaApiInexistenteMesmoComAcceptHtml_DeveManterJson404() throws Exception {
        mockMvc.perform(get("/api/rota-inexistente").accept(MediaType.TEXT_HTML, MediaType.ALL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"));
    }
}
