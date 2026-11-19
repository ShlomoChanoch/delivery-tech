package com.deliverytech.delivery_api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliverytech.delivery_api.repository.ClienteRepository;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RelatorioVendas;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import com.deliverytech.delivery_api.service.impl.RelatorioServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Contrato dos templates do painel (§3 do handover): chaves exatas dos 5 maps, escolha da variante
 * de período e null-guards. Rename de chave quebra o painel silenciosamente — por isso as asserções
 * de {@code keySet()} inteiro, não só presença da chave.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceImplTest {

    @Mock private RestauranteRepository restauranteRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private PedidoRepository pedidoRepository;

    @InjectMocks private RelatorioServiceImpl service;

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 11, 19);

    // === relatorioVendasPorRestaurante ===

    @Test
    @DisplayName("Vendas por restaurante: sem período usa a variante geral e devolve o contrato")
    void vendasPorRestaurante_SemPeriodo_VarianteGeral() {
        RelatorioVendas linha = org.mockito.Mockito.mock(RelatorioVendas.class);
        when(linha.getNomeRestaurante()).thenReturn("Pizza Express");
        when(linha.getQuantidadePedidos()).thenReturn(4L);
        when(linha.getTotalVendas()).thenReturn(new BigDecimal("227.20"));
        when(restauranteRepository.relatorioVendasPorRestaurante()).thenReturn(List.of(linha));

        List<Map<String, Object>> resultado = service.relatorioVendasPorRestaurante(null, null);

        verify(restauranteRepository).relatorioVendasPorRestaurante();
        verify(restauranteRepository, never())
                .relatorioVendasPorRestaurantePorPeriodo(any(), any());
        assertEquals(1, resultado.size());
        assertEquals(
                Set.of("restaurante", "quantidadePedidos", "totalVendas"),
                resultado.get(0).keySet());
        assertEquals("Pizza Express", resultado.get(0).get("restaurante"));
        assertEquals(4L, resultado.get(0).get("quantidadePedidos"));
        assertEquals(new BigDecimal("227.20"), resultado.get(0).get("totalVendas"));
    }

    @Test
    @DisplayName("Vendas por restaurante: com período usa a variante com bounds corretos")
    void vendasPorRestaurante_ComPeriodo_VarianteComBounds() {
        when(restauranteRepository.relatorioVendasPorRestaurantePorPeriodo(any(), any()))
                .thenReturn(List.of());

        service.relatorioVendasPorRestaurante(INICIO, FIM);

        // início inclusivo: atStartOfDay; fim exclusivo: dia seguinte cobre o dia inteiro
        verify(restauranteRepository)
                .relatorioVendasPorRestaurantePorPeriodo(
                        LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 20, 0, 0));
        verify(restauranteRepository, never()).relatorioVendasPorRestaurante();
    }

    @Test
    @DisplayName("Vendas por restaurante: null-guards (SUM null vira ZERO, nome null vira '')")
    void vendasPorRestaurante_NulGuards() {
        RelatorioVendas linha = org.mockito.Mockito.mock(RelatorioVendas.class);
        when(restauranteRepository.relatorioVendasPorRestaurante()).thenReturn(List.of(linha));

        List<Map<String, Object>> resultado = service.relatorioVendasPorRestaurante(null, null);

        assertEquals("", resultado.get(0).get("restaurante"));
        assertEquals(0L, resultado.get(0).get("quantidadePedidos"));
        assertEquals(BigDecimal.ZERO, resultado.get(0).get("totalVendas"));
    }

    // === relatorioProdutosMaisVendidos ===

    @Test
    @DisplayName("Produtos mais vendidos: contrato e conversão das colunas Object[]")
    void produtosMaisVendidos_Contrato() {
        when(produtoRepository.produtosMaisVendidos(10))
                .thenReturn(
                        List.<Object[]>of(
                                new Object[] {"Pizza Margherita", 3L, new BigDecimal("77.70")}));

        List<Map<String, Object>> resultado = service.relatorioProdutosMaisVendidos(10, null, null);

        verify(produtoRepository).produtosMaisVendidos(10);
        verify(produtoRepository, never()).produtosMaisVendidosPorPeriodo(anyInt(), any(), any());
        assertEquals(
                Set.of("produto", "quantidadeVendida", "totalArrecadado"),
                resultado.get(0).keySet());
        assertEquals("Pizza Margherita", resultado.get(0).get("produto"));
        assertEquals(3L, resultado.get(0).get("quantidadeVendida"));
        assertEquals(new BigDecimal("77.70"), resultado.get(0).get("totalArrecadado"));
    }

    @Test
    @DisplayName("Produtos mais vendidos: com período passa limite e bounds para a variante")
    void produtosMaisVendidos_ComPeriodo() {
        when(produtoRepository.produtosMaisVendidosPorPeriodo(anyInt(), any(), any()))
                .thenReturn(List.<Object[]>of());

        service.relatorioProdutosMaisVendidos(5, INICIO, FIM);

        verify(produtoRepository)
                .produtosMaisVendidosPorPeriodo(
                        eq(5),
                        eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                        eq(LocalDateTime.of(2026, 11, 20, 0, 0)));
    }

    @Test
    @DisplayName("Produtos mais vendidos: coluna numérica nula vira 0, decimal nula vira ZERO")
    void produtosMaisVendidos_NulGuards() {
        when(produtoRepository.produtosMaisVendidos(10))
                .thenReturn(List.<Object[]>of(new Object[] {null, null, null}));

        List<Map<String, Object>> resultado = service.relatorioProdutosMaisVendidos(10, null, null);

        assertEquals("", resultado.get(0).get("produto"));
        assertEquals(0L, resultado.get(0).get("quantidadeVendida"));
        assertEquals(BigDecimal.ZERO, resultado.get(0).get("totalArrecadado"));
    }

    // === relatorioClientesAtivos ===

    @Test
    @DisplayName("Clientes ativos: contrato e escolha da variante")
    void clientesAtivos_Contrato() {
        when(clienteRepository.rankingClientesPorPedidos(10))
                .thenReturn(
                        List.<Object[]>of(
                                new Object[] {"João Silva", 5L, new BigDecimal("125.00")}));

        List<Map<String, Object>> resultado = service.relatorioClientesAtivos(10, null, null);

        verify(clienteRepository).rankingClientesPorPedidos(10);
        verify(clienteRepository, never())
                .rankingClientesPorPedidosPorPeriodo(anyInt(), any(), any());
        assertEquals(
                Set.of("cliente", "quantidadePedidos", "totalGasto"), resultado.get(0).keySet());
        assertEquals("João Silva", resultado.get(0).get("cliente"));
        assertEquals(5L, resultado.get(0).get("quantidadePedidos"));
        assertEquals(new BigDecimal("125.00"), resultado.get(0).get("totalGasto"));
    }

    @Test
    @DisplayName("Clientes ativos: com período usa a variante com bounds corretos")
    void clientesAtivos_ComPeriodo() {
        when(clienteRepository.rankingClientesPorPedidosPorPeriodo(anyInt(), any(), any()))
                .thenReturn(List.<Object[]>of());

        service.relatorioClientesAtivos(10, INICIO, FIM);

        verify(clienteRepository)
                .rankingClientesPorPedidosPorPeriodo(
                        eq(10),
                        eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                        eq(LocalDateTime.of(2026, 11, 20, 0, 0)));
    }

    // === relatorioPedidosPorPeriodo ===

    @Test
    @DisplayName("Pedidos por período: contrato, agrupamento null vira 'total'")
    void pedidosPorPeriodo_AgrupamentoPadrao() {
        when(pedidoRepository.contarVendasPorPeriodo(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {12L, new BigDecimal("774.10")}));

        Map<String, Object> resultado = service.relatorioPedidosPorPeriodo(INICIO, FIM, null);

        assertEquals(Set.of("totalPedidos", "valorTotal", "agrupamento"), resultado.keySet());
        assertEquals(12L, resultado.get("totalPedidos"));
        assertEquals(new BigDecimal("774.10"), resultado.get("valorTotal"));
        assertEquals("total", resultado.get("agrupamento"));
    }

    @Test
    @DisplayName("Pedidos por período: agrupamento informado é repassado e agregado vazio zera")
    void pedidosPorPeriodo_AgrupamentoInformadoEAgregadoVazio() {
        when(pedidoRepository.contarVendasPorPeriodo(any(), any())).thenReturn(List.<Object[]>of());

        Map<String, Object> resultado = service.relatorioPedidosPorPeriodo(INICIO, FIM, "dia");

        assertEquals("dia", resultado.get("agrupamento"));
        // Lista vazia (cenário anômalo) não pode estourar índice nem virar null
        assertEquals(0L, resultado.get("totalPedidos"));
        assertEquals(BigDecimal.ZERO, resultado.get("valorTotal"));
    }

    // === resumoVendas ===

    @Test
    @DisplayName("Resumo: sem período usa contarVendasGeral e devolve o contrato do dashboard")
    void resumoVendas_SemPeriodo() {
        when(pedidoRepository.contarVendasGeral())
                .thenReturn(List.<Object[]>of(new Object[] {12L, new BigDecimal("774.10")}));
        when(restauranteRepository.countByAtivoTrue()).thenReturn(5L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(4L);

        Map<String, Object> resultado = service.resumoVendas(null, null);

        verify(pedidoRepository).contarVendasGeral();
        verify(pedidoRepository, never()).contarVendasPorPeriodo(any(), any());
        assertEquals(
                Set.of(
                        "totalPedidos",
                        "valorTotalVendas",
                        "ticketMedio",
                        "restaurantesAtivos",
                        "clientesAtivos"),
                resultado.keySet());
        assertEquals(12L, resultado.get("totalPedidos"));
        assertEquals(new BigDecimal("774.10"), resultado.get("valorTotalVendas"));
        // 774,10 / 12 = 64,5083... → HALF_UP com 2 casas
        assertEquals(new BigDecimal("64.51"), resultado.get("ticketMedio"));
        assertEquals(5L, resultado.get("restaurantesAtivos"));
        assertEquals(4L, resultado.get("clientesAtivos"));
    }

    @Test
    @DisplayName("Resumo: com período usa a variante com bounds corretos")
    void resumoVendas_ComPeriodo() {
        when(pedidoRepository.contarVendasPorPeriodo(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {3L, new BigDecimal("90.00")}));
        when(restauranteRepository.countByAtivoTrue()).thenReturn(1L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(1L);

        service.resumoVendas(INICIO, FIM);

        verify(pedidoRepository)
                .contarVendasPorPeriodo(
                        LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 20, 0, 0));
        verify(pedidoRepository, never()).contarVendasGeral();
    }

    @Test
    @DisplayName("Resumo: 0 pedidos → ticketMedio ZERO (não divide por zero) e SUM null vira ZERO")
    void resumoVendas_ZeroPedidos() {
        when(pedidoRepository.contarVendasGeral())
                .thenReturn(List.<Object[]>of(new Object[] {0L, null}));
        when(restauranteRepository.countByAtivoTrue()).thenReturn(0L);
        when(clienteRepository.countByAtivoTrue()).thenReturn(0L);

        Map<String, Object> resultado = service.resumoVendas(null, null);

        assertEquals(0L, resultado.get("totalPedidos"));
        assertEquals(BigDecimal.ZERO, resultado.get("valorTotalVendas"));
        assertEquals(BigDecimal.ZERO, resultado.get("ticketMedio"));
        assertTrue(resultado.get("ticketMedio") instanceof BigDecimal);
    }
}
