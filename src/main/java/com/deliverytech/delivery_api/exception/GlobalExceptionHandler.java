package com.deliverytech.delivery_api.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Tratamento global de exceções para toda a aplicação */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String RECURSO_NAO_ENCONTRADO = "Recurso não encontrado";

    /** Tratamento para validações de Bean Validation (@NotBlank, @Email, etc.) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
        errors.put(STATUS, HttpStatus.BAD_REQUEST.value());
        errors.put(ERROR, "Dados inválidos");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        errors.put("validationErrors", fieldErrors);

        log.warn("Erro de validação: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }

    /** Tratamento para argumentos ilegais (cliente não encontrado, etc.) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, "Requisição inválida");
        error.put(MESSAGE, ex.getMessage());

        log.warn("Erro de argumento ilegal: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /** Tratamento para rotas e recursos estáticos inexistentes (Spring MVC 6.1+) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException ex) {

        log.warn("Recurso estático/rota não encontrado: {}", ex.getMessage());
        return buildNotFoundResponse(ex.getMessage());
    }

    /** Tratamento para entidades de domínio não encontradas */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(
            EntityNotFoundException ex) {

        log.warn("Entidade não encontrada: {}", ex.getMessage());
        return buildNotFoundResponse(ex.getMessage());
    }

    /** Tratamento para recursos não encontrados */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {

        String message = ex.getMessage();
        // Se for "não encontrado", retornar 404
        if (message != null
                && (message.contains("não encontrado") || message.contains("not found"))) {

            log.warn("Recurso não encontrado: {}", message);
            return buildNotFoundResponse(message);
        }

        // Outros RuntimeExceptions
        return handleGenericException(ex);
    }

    private ResponseEntity<Map<String, Object>> buildNotFoundResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, RECURSO_NAO_ENCONTRADO);
        error.put(MESSAGE, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Tratamento para violações de constraint */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, "Violação de restrição");
        error.put(MESSAGE, ex.getMessage());

        log.warn("Violação de constraint: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /** Tratamento genérico para exceções não tratadas */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {

        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
        error.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(ERROR, "Erro interno do servidor");
        error.put(MESSAGE, "Ocorreu um erro inesperado");

        log.error("Erro interno: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
