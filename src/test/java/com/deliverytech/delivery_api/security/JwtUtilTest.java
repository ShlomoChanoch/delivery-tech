package com.deliverytech.delivery_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regressão da Fase 1: o segredo JWT deve vir de configuração externa (JWT_SECRET), ser uma chave
 * Base64URL válida e permitir assinar/verificar tokens. O valor usado aqui é descartável, gerado
 * apenas para o teste — não é segredo de nenhum ambiente.
 */
class JwtUtilTest {

    private static final String SEGREDO_TESTE =
            Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            "chave-de-teste-com-32-bytes-ok!!".getBytes(StandardCharsets.UTF_8));

    private JwtUtil novoJwtUtil(String secret) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);
        return jwtUtil;
    }

    @Test
    void deveAssinarEVerificarTokenComSegredoBase64Url() {
        JwtUtil jwtUtil = novoJwtUtil(SEGREDO_TESTE);

        String token = jwtUtil.gerarToken("usuario@teste.com");
        Claims claims = jwtUtil.validarToken(token);

        assertNotNull(token);
        assertNotNull(claims);
        assertEquals("usuario@teste.com", claims.getSubject());
        assertEquals("usuario@teste.com", jwtUtil.getEmailFromToken(token));
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        String token = novoJwtUtil(SEGREDO_TESTE).gerarToken("usuario@teste.com");
        String outroSegredo =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                "outra-chave-de-teste-32-bytes!!!"
                                        .getBytes(StandardCharsets.UTF_8));

        assertNull(novoJwtUtil(outroSegredo).validarToken(token));
    }

    @Test
    void deveFalharCedoQuandoSegredoNaoForBase64UrlValido() {
        JwtUtil jwtUtil = novoJwtUtil("nao-e-base64url!!!");

        assertThrows(RuntimeException.class, jwtUtil::validarConfiguracao);
        assertThrows(RuntimeException.class, () -> jwtUtil.gerarToken("usuario@teste.com"));
    }

    @Test
    void deveFalharCedoQuandoSegredoForMenorQue256Bits() {
        String curta =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("curta-demais".getBytes(StandardCharsets.UTF_8));
        JwtUtil jwtUtil = novoJwtUtil(curta);

        assertThrows(RuntimeException.class, jwtUtil::validarConfiguracao);
    }
}
