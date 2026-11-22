package com.deliverytech.delivery_api.controller.web;

import com.deliverytech.delivery_api.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tratamento de erros amigável em HTML para o painel web Thymeleaf ({@link PainelController}).
 * Escopado ao controller web com precedência máxima para não interferir no envelope JSON da API
 * REST.
 */
@Slf4j
@ControllerAdvice(assignableTypes = PainelController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PainelExceptionHandler {

    private static final String VIEW_ERRO = "painel/erro";

    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        String caminho = request != null ? request.getRequestURI() : "";
        log.warn("Recurso não encontrado no painel ({}): {}", caminho, ex.getMessage());
        return criarPaginaErro(
                HttpStatus.NOT_FOUND,
                "Página não encontrada",
                "O registro solicitado não foi encontrado",
                "Verifique os filtros utilizados ou volte para o dashboard.",
                caminho);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        String caminho = request != null ? request.getRequestURI() : "";
        log.error("Erro inesperado ao renderizar o painel ({})", caminho, ex);
        return criarPaginaErro(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro no painel",
                "Não foi possível carregar esta página",
                "Ocorreu uma falha temporária ao consultar os dados. Tente novamente em instantes.",
                caminho);
    }

    public static ModelAndView criarPaginaErro(
            HttpStatus status,
            String tituloErro,
            String subtituloErro,
            String mensagemErro,
            String caminho) {
        ModelAndView mv = new ModelAndView(VIEW_ERRO);
        mv.setStatus(status);
        mv.addObject("statusCode", status.value());
        mv.addObject("tituloErro", tituloErro);
        mv.addObject("subtituloErro", subtituloErro);
        mv.addObject("mensagemErro", mensagemErro);
        mv.addObject("caminho", caminho);
        return mv;
    }
}
