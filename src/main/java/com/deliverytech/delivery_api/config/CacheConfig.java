package com.deliverytech.delivery_api.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita o cache Spring com Caffeine (especificação em {@code spring.cache.caffeine.spec}). Em
 * testes o profile {@code test} define {@code spring.cache.type=none} para isolar os cenários.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    public static final String RELATORIO_VENDAS_RESTAURANTE = "relatorioVendasPorRestaurante";
    public static final String RELATORIO_PRODUTOS_MAIS_VENDIDOS = "relatorioProdutosMaisVendidos";
    public static final String RELATORIO_CLIENTES_ATIVOS = "relatorioClientesAtivos";
    public static final String RELATORIO_PEDIDOS_PERIODO = "relatorioPedidosPorPeriodo";
    public static final String RELATORIO_RESUMO_VENDAS = "relatorioResumoVendas";

    private CacheConfig() {}
}
