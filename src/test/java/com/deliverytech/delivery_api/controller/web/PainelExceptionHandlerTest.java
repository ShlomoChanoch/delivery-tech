package com.deliverytech.delivery_api.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.deliverytech.delivery_api.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

class PainelExceptionHandlerTest {

    private PainelExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PainelExceptionHandler();
    }

    @Test
    @DisplayName("EntityNotFoundException no painel deve retornar view painel/erro com status 404")
    void handleEntityNotFound_DeveRetornar404ComViewErro() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/painel/clientes");
        EntityNotFoundException ex = new EntityNotFoundException("Cliente", 99L);

        ModelAndView mv = handler.handleEntityNotFound(ex, request);

        assertEquals("painel/erro", mv.getViewName());
        assertEquals(HttpStatus.NOT_FOUND, mv.getStatus());
        assertEquals(404, mv.getModel().get("statusCode"));
        assertEquals("Página não encontrada", mv.getModel().get("tituloErro"));
        assertEquals("/painel/clientes", mv.getModel().get("caminho"));
    }

    @Test
    @DisplayName("Exceção genérica no painel deve retornar 500 sem expor detalhes internos")
    void handleGenericException_DeveRetornar500SemVazarDetalhes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/painel/relatorios");
        RuntimeException ex = new RuntimeException("SQL syntax error near SELECT");

        ModelAndView mv = handler.handleGenericException(ex, request);

        assertEquals("painel/erro", mv.getViewName());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, mv.getStatus());
        assertEquals(500, mv.getModel().get("statusCode"));
        assertEquals("Erro no painel", mv.getModel().get("tituloErro"));
        assertEquals("/painel/relatorios", mv.getModel().get("caminho"));
        String mensagem = String.valueOf(mv.getModel().get("mensagemErro"));
        assertNotNull(mensagem);
        assertFalse(mensagem.contains("SQL syntax error"));
    }
}
