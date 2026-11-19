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

/** Tratamento global de exceções para toda a aplicação */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

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

    /** Tratamento para recursos não encontrados */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {

        // Se for "não encontrado", retornar 404
        if (ex.getMessage().contains("não encontrado") || ex.getMessage().contains("not found")) {

            Map<String, Object> error = new HashMap<>();
            error.put(TIMESTAMP, LocalDateTime.now(ZoneId.systemDefault()));
            error.put(STATUS, HttpStatus.NOT_FOUND.value());
            error.put(ERROR, "Recurso não encontrado");
            error.put(MESSAGE, ex.getMessage());

            log.warn("Recurso não encontrado: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        }

        // Outros RuntimeExceptions
        return handleGenericException(ex);
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
