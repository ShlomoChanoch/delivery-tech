package com.deliverytech.delivery_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Resposta quando realizar o login", title = "Login Response DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    @Schema(description = "Token JWT gerado para o usuário autenticado")
    private String token;

    @Schema(description = "Usuário autenticado")
    private String username;

    @Schema(description = "Mensagem descritiva do resultado")
    private String message;
}
