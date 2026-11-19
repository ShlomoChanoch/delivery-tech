package com.deliverytech.delivery_api.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Validação de Bean Validation deve retornar 400 com erros por campo")
    void testHandleValidationExceptions_DeveRetornar400ComErrosDeCampo() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("clienteRequest", "email", "Email inválido")));

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        // Then
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertInstanceOf(LocalDateTime.class, body.get("timestamp"));
        assertEquals(400, body.get("status"));
        assertEquals("Dados inválidos", body.get("error"));
        @SuppressWarnings("unchecked")
        Map<String, String> validationErrors = (Map<String, String>) body.get("validationErrors");
        assertEquals("Email inválido", validationErrors.get("email"));
    }

    @Test
    @DisplayName("Argumento ilegal deve retornar 400 com a mensagem da exceção")
    void testHandleIllegalArgumentException_DeveRetornar400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        // Then
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Requisição inválida", body.get("error"));
        assertEquals("Argumento inválido", body.get("message"));
    }

    @Test
    @DisplayName("RuntimeException com 'não encontrado' deve retornar 404")
    void testHandleRuntimeException_NaoEncontrado_DeveRetornar404() {
        // When
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(new RuntimeException("Cliente não encontrado"));

        // Then
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("RuntimeException com 'not found' deve retornar 404")
    void testHandleRuntimeException_NotFound_DeveRetornar404() {
        // When
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(new RuntimeException("Resource not found"));

        // Then
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("RuntimeException genérica deve delegar ao handler genérico e retornar 500")
    void testHandleRuntimeException_OutroErro_DeveRetornar500() {
        // When
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(new RuntimeException("Falha inesperada"));

        // Then
        assertEquals(500, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Erro interno do servidor", body.get("error"));
        assertEquals("Ocorreu um erro inesperado", body.get("message"));
    }

    @Test
    @DisplayName("Violação de constraint deve retornar 400 com a mensagem da exceção")
    void testHandleConstraintViolationException_DeveRetornar400() {
        // Given
        ConstraintViolationException ex =
                new ConstraintViolationException("CEP inválido", Collections.emptySet());

        // When
        ResponseEntity<Map<String, Object>> response =
                handler.handleConstraintViolationException(ex);

        // Then
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Violação de restrição", body.get("error"));
        assertEquals("CEP inválido", body.get("message"));
    }

    @Test
    @DisplayName("Exceção genérica deve retornar 500 sem expor detalhes internos")
    void testHandleGenericException_DeveRetornar500() {
        // When
        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(new Exception("Detalhe interno"));

        // Then
        assertEquals(500, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Erro interno do servidor", body.get("error"));
        assertEquals("Ocorreu um erro inesperado", body.get("message"));
    }
}
