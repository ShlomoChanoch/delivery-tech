package com.deliverytech.delivery_api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Cobre as queries nativas de {@link ClienteRepository} (código morto antes deste ciclo). Além do
 * SQL em H2, valida o contrato de filtro de ativos e o {@code COALESCE} do total gasto.
 */
@DataJpaTest
@ActiveProfiles("test")
class ClienteRepositoryTest {

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;

    private Restaurante restaurante;
    private Produto produto;
    private LocalDateTime agora;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();
        restaurante =
                restauranteRepository.save(
                        Restaurante.builder().nome("Rest Teste").categoria("Pizza").build());
        produto =
                produtoRepository.save(
                        Produto.builder()
                                .nome("Produto Teste")
                                .categoria("Pizza")
                                .preco(new BigDecimal("25.00"))
                                .restaurante(restaurante)
                                .build());
    }

    @Test
    @DisplayName("Cliente inativo não deve aparecer no ranking mesmo com pedidos")
    void clienteInativoNaoAparece() {
        Cliente ativo = cliente("Ativo", "ativo@teste.com", true);
        Cliente inativo = cliente("Inativo", "inativo@teste.com", false);
        vender(ativo, 1);
        vender(inativo, 3);

        List<Object[]> resultado = clienteRepository.rankingClientesPorPedidos(10);

        assertEquals(1, resultado.size(), "inativo é filtrado por WHERE c.ativo = TRUE");
        assertEquals("Ativo", resultado.get(0)[0]);
    }

    @Test
    @DisplayName("Deve ordenar por nº de pedidos desc e respeitar o limite")
    void deveOrdenarPorPedidosDescComLimite() {
        Cliente top = cliente("Top", "top@teste.com", true);
        Cliente meio = cliente("Meio", "meio@teste.com", true);
        cliente("Vazio", "vazio@teste.com", true);
        vender(top, 3);
        vender(meio, 1);

        List<Object[]> resultado = clienteRepository.rankingClientesPorPedidos(2);

        assertEquals(2, resultado.size());
        assertEquals("Top", resultado.get(0)[0]);
        assertEquals(3L, resultado.get(0)[1]);
        assertEquals("Meio", resultado.get(1)[0]);
        assertEquals(1L, resultado.get(1)[1]);
    }

    @Test
    @DisplayName("totalGasto deve somar valorTotal dos pedidos do cliente")
    void totalGastoSomaValorTotal() {
        Cliente cliente = cliente("Cliente", "cliente@teste.com", true);
        vender(cliente, 2); // 2 pedidos de R$ 25,00

        List<Object[]> resultado = clienteRepository.rankingClientesPorPedidos(10);

        assertEquals(1, resultado.size());
        assertEquals(
                0,
                new BigDecimal(resultado.get(0)[2].toString()).compareTo(new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Cliente ativo sem pedidos aparece com contagem 0 e gasto 0 (LEFT JOIN)")
    void clienteAtivoSemPedidosApareceComZero() {
        cliente("Sem Pedidos", "sem@teste.com", true);

        List<Object[]> resultado = clienteRepository.rankingClientesPorPedidos(10);

        assertEquals(1, resultado.size());
        assertEquals("Sem Pedidos", resultado.get(0)[0]);
        assertEquals(0L, resultado.get(0)[1]);
        assertEquals(0, new BigDecimal(resultado.get(0)[2].toString()).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Variante com período deve contar só pedidos dentro da janela")
    void varianteComPeriodoContaSoPedidosDaJanela() {
        Cliente cliente = cliente("Cliente", "cliente@teste.com", true);
        vender(cliente, 1, agora.minusDays(2)); // dentro
        vender(cliente, 1, agora.minusDays(45)); // fora

        List<Object[]> resultado =
                clienteRepository.rankingClientesPorPedidosPorPeriodo(
                        10, agora.minusDays(30), agora.plusDays(1));

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0)[1], "só o pedido da janela conta");
        assertEquals(
                0,
                new BigDecimal(resultado.get(0)[2].toString()).compareTo(new BigDecimal("25.00")));
    }

    @Test
    @DisplayName("countByAtivoTrue deve contar apenas ativos")
    void countPorAtivoTrue() {
        cliente("Ativo 1", "a1@teste.com", true);
        cliente("Ativo 2", "a2@teste.com", true);
        cliente("Inativo", "i1@teste.com", false);

        assertEquals(2, clienteRepository.countByAtivoTrue());
    }

    @Test
    @DisplayName("Banco vazio deve devolver lista vazia, nunca null")
    void bancoVazioDevolveListaVazia() {
        assertTrue(clienteRepository.rankingClientesPorPedidos(10).isEmpty());
        assertTrue(
                clienteRepository
                        .rankingClientesPorPedidosPorPeriodo(
                                10, agora.minusDays(30), agora.plusDays(1))
                        .isEmpty());
    }

    private Cliente cliente(String nome, String email, boolean ativo) {
        return clienteRepository.save(
                Cliente.builder().nome(nome).email(email).ativo(ativo).build());
    }

    private void vender(Cliente cliente, int vezes) {
        vender(cliente, vezes, agora.minusDays(1));
    }

    private void vender(Cliente cliente, int vezes, LocalDateTime dataPedido) {
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
