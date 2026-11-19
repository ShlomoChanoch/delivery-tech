package com.deliverytech.delivery_api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.deliverytech.delivery_api.model.Cliente;
import com.deliverytech.delivery_api.model.ItemPedido;
import com.deliverytech.delivery_api.model.Pedido;
import com.deliverytech.delivery_api.model.Produto;
import com.deliverytech.delivery_api.model.Restaurante;
import com.deliverytech.delivery_api.model.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Cobre {@link RestauranteRepository#relatorioVendasPorRestaurante()} e a variante com período. O
 * teste de regressão do alias é o mais importante do ciclo: o SQL antigo projetava {@code
 * quantidePedidos} (typo) e o getter esperava {@code quantidadePedidos} — a contagem vinha {@code
 * null} e o template do dashboard dividia por zero em silêncio.
 */
@DataJpaTest
@ActiveProfiles("test")
class RestauranteRepositoryTest {

    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;

    private Cliente cliente;
    private LocalDateTime agora;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();
        cliente =
                clienteRepository.save(
                        Cliente.builder()
                                .nome("Cliente Teste")
                                .email("cliente@teste.com")
                                .ativo(true)
                                .build());
    }

    @Test
    @DisplayName("Regressão do alias: getQuantidadePedidos() não pode vir null")
    void regressaoDoAliasQuantidadePedidos() {
        Restaurante restaurante = restaurante("Pizza Express");
        vender(restaurante, 2, agora.minusDays(1));

        List<RelatorioVendas> resultado = restauranteRepository.relatorioVendasPorRestaurante();

        assertEquals(1, resultado.size());
        // Com o alias 'quantidePedidos' o getter devolvia null (bug histórico)
        assertNotNull(
                resultado.get(0).getQuantidadePedidos(),
                "alias corrigido para 'quantidadePedidos'");
        assertEquals(2L, resultado.get(0).getQuantidadePedidos());
    }

    @Test
    @DisplayName("Restaurante sem pedidos deve aparecer com contagem 0 e vendas null/zero")
    void restauranteSemPedidosApareceComZero() {
        restaurante("Sem Pedidos");

        List<RelatorioVendas> resultado = restauranteRepository.relatorioVendasPorRestaurante();

        assertEquals(1, resultado.size());
        assertNotNull(resultado.get(0).getQuantidadePedidos());
        assertEquals(0L, resultado.get(0).getQuantidadePedidos());
        // SUM vira null em LEFT JOIN sem pedidos — o RelatorioServiceImpl tem null-guard
        // (Map.of não aceita null); aqui só registramos o comportamento do dado bruto
    }

    @Test
    @DisplayName(
            "Deve ordenar por nº de pedidos desc (barra do dashboard usa lista[0] como máximo)")
    void deveOrdenarPorPedidosDesc() {
        Restaurante top = restaurante("Rest A");
        Restaurante meio = restaurante("Rest B");
        restaurante("Rest C");
        vender(top, 3, agora.minusDays(1));
        vender(meio, 1, agora.minusDays(1));

        List<RelatorioVendas> resultado = restauranteRepository.relatorioVendasPorRestaurante();

        assertEquals(3, resultado.size());
        assertEquals("Rest A", resultado.get(0).getNomeRestaurante());
        assertEquals(3L, resultado.get(0).getQuantidadePedidos());
        assertEquals("Rest B", resultado.get(1).getNomeRestaurante());
        assertEquals(1L, resultado.get(1).getQuantidadePedidos());
        assertEquals("Rest C", resultado.get(2).getNomeRestaurante());
        assertEquals(0L, resultado.get(2).getQuantidadePedidos());
    }

    @Test
    @DisplayName("totalVendas deve somar valorTotal dos pedidos")
    void totalVendasSomaValorTotal() {
        Restaurante restaurante = restaurante("Rest Soma");
        vender(restaurante, 2, agora.minusDays(1)); // 2 pedidos de R$ 30,00

        List<RelatorioVendas> resultado = restauranteRepository.relatorioVendasPorRestaurante();

        assertEquals(1, resultado.size());
        assertNotNull(resultado.get(0).getTotalVendas());
        assertEquals(0, resultado.get(0).getTotalVendas().compareTo(new BigDecimal("60.00")));
    }

    @Test
    @DisplayName("Variante com período deve contar só pedidos dentro da janela")
    void varianteComPeriodoContaSoJanela() {
        Restaurante restaurante = restaurante("Rest Período");
        vender(restaurante, 1, agora.minusDays(2)); // dentro
        vender(restaurante, 4, agora.minusDays(60)); // fora

        List<RelatorioVendas> resultado =
                restauranteRepository.relatorioVendasPorRestaurantePorPeriodo(
                        agora.minusDays(30), agora.plusDays(1));

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getQuantidadePedidos());
        assertEquals(0, resultado.get(0).getTotalVendas().compareTo(new BigDecimal("30.00")));
    }

    @Test
    @DisplayName("countByAtivoTrue deve contar apenas restaurantes ativos")
    void countPorAtivoTrue() {
        restaurante("Ativo 1").setAtivo(true);
        Restaurante inativo = restaurante("Inativo");
        inativo.setAtivo(false);
        restauranteRepository.save(inativo);

        assertEquals(1, restauranteRepository.countByAtivoTrue());
    }

    @Test
    @DisplayName("Banco vazio deve devolver lista vazia, nunca null")
    void bancoVazioDevolveListaVazia() {
        assertTrue(restauranteRepository.relatorioVendasPorRestaurante().isEmpty());
        assertTrue(
                restauranteRepository
                        .relatorioVendasPorRestaurantePorPeriodo(
                                agora.minusDays(30), agora.plusDays(1))
                        .isEmpty());
    }

    private Restaurante restaurante(String nome) {
        return restauranteRepository.save(
                Restaurante.builder()
                        .nome(nome)
                        .categoria("Pizza")
                        .avaliacao(new BigDecimal("4.5"))
                        .taxaEntrega(BigDecimal.ZERO)
                        .ativo(true)
                        .build());
    }

    private void vender(Restaurante restaurante, int vezes, LocalDateTime dataPedido) {
        Produto produto =
                produtoRepository.save(
                        Produto.builder()
                                .nome("Produto de " + restaurante.getNome())
                                .categoria("Pizza")
                                .preco(new BigDecimal("30.00"))
                                .restaurante(restaurante)
                                .build());
        for (int i = 0; i < vezes; i++) {
            Pedido pedido =
                    Pedido.builder()
                            .cliente(cliente)
                            .restaurante(restaurante)
                            .status(StatusPedido.ENTREGUE)
                            .dataPedido(dataPedido)
                            .valorTotal(produto.getPreco())
                            .build();
            ItemPedido item =
                    ItemPedido.builder()
                            .pedido(pedido)
                            .produto(produto)
                            .quantidade(1)
                            .precoUnitario(produto.getPreco())
                            .build();
            item.setSubtotal();
            pedido.setItens(List.of(item));
            pedidoRepository.saveAndFlush(pedido);
        }
    }
}
