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
 * Cobre as queries nativas de {@link ProdutoRepository} — antes deste ciclo nenhum caminho de
 * produção ou teste as executava (código morto). O que só falha na execução e é pego aqui: nomes de
 * coluna em H2, {@code LIMIT :limite} parametrizado e a semântica do LEFT JOIN.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProdutoRepositoryTest {

    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PedidoRepository pedidoRepository;

    private Restaurante restaurante;
    private Cliente cliente;
    private LocalDateTime agora;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();
        restaurante =
                restauranteRepository.save(
                        Restaurante.builder().nome("Rest Teste").categoria("Pizza").build());
        cliente =
                clienteRepository.save(
                        Cliente.builder()
                                .nome("Cliente Teste")
                                .email("cliente@teste.com")
                                .ativo(true)
                                .build());
    }

    @Test
    @DisplayName("Deve ordenar por vendas decrescentes e respeitar o limite parametrizado")
    void deveOrdenarPorVendidosDescComLimite() {
        // 7 produtos com vendas decrescentes: A=5, B=4, C=3, D=2, E=1, F=0, G=0
        Produto a = produto("Produto A", "10.00");
        Produto b = produto("Produto B", "20.00");
        Produto c = produto("Produto C", "30.00");
        Produto d = produto("Produto D", "40.00");
        Produto e = produto("Produto E", "50.00");
        produto("Produto F", "60.00");
        produto("Produto G", "70.00");
        vender(a, agora.minusDays(1), 5);
        vender(b, agora.minusDays(1), 4);
        vender(c, agora.minusDays(1), 3);
        vender(d, agora.minusDays(1), 2);
        vender(e, agora.minusDays(1), 1);

        List<Object[]> resultado = produtoRepository.produtosMaisVendidos(5);

        assertEquals(5, resultado.size(), "limite 5 sobre 7 produtos");
        assertEquals("Produto A", resultado.get(0)[0]);
        assertEquals(5L, resultado.get(0)[1]);
        assertEquals("Produto B", resultado.get(1)[0]);
        assertEquals(4L, resultado.get(1)[1]);
        assertEquals("Produto C", resultado.get(2)[0]);
        assertEquals(3L, resultado.get(2)[1]);
        assertEquals("Produto D", resultado.get(3)[0]);
        assertEquals(2L, resultado.get(3)[1]);
        assertEquals("Produto E", resultado.get(4)[0]);
        assertEquals(1L, resultado.get(4)[1]);
    }

    @Test
    @DisplayName("Produto sem venda deve aparecer com contagem 0 e arrecadado 0 (LEFT JOIN)")
    void deveIncluirProdutoSemVendaComZero() {
        produto("Produto A", "10.00");
        produto("Produto Sem Venda Z", "99.00");

        List<Object[]> resultado = produtoRepository.produtosMaisVendidos(10);

        assertEquals(2, resultado.size());
        Object[] semVenda = resultado.get(1);
        assertEquals("Produto Sem Venda Z", semVenda[0]);
        assertEquals(0L, semVenda[1]);
        assertEquals(
                0,
                new BigDecimal(semVenda[2].toString()).compareTo(BigDecimal.ZERO),
                "COALESCE deve devolver zero, não null (quebra Map.of no service)");
    }

    @Test
    @DisplayName("totalArrecadado deve ser a soma dos subtotais dos itens vendidos")
    void deveSomarTotalArrecadado() {
        Produto a = produto("Produto A", "10.00");
        vender(a, agora.minusDays(1), 3); // 3 itens de R$ 10,00

        List<Object[]> resultado = produtoRepository.produtosMaisVendidos(10);

        assertEquals(1, resultado.size());
        assertEquals(
                0,
                new BigDecimal(resultado.get(0)[2].toString()).compareTo(new BigDecimal("30.00")));
    }

    @Test
    @DisplayName(
            "Variante com período deve excluir venda fora da janela e produto sem venda no período")
    void varianteComPeriodoExcluiVendaForaDaJanela() {
        Produto a = produto("Produto A", "10.00");
        produto("Produto Nunca Vendeu", "10.00");
        vender(a, agora.minusDays(1), 2); // dentro da janela
        vender(a, agora.minusDays(60), 3); // fora da janela

        LocalDateTime inicio = agora.minusDays(30);
        LocalDateTime fim = agora.plusDays(1);
        List<Object[]> resultado =
                produtoRepository.produtosMaisVendidosPorPeriodo(10, inicio, fim);

        assertEquals(1, resultado.size(), "produto sem venda no período não aparece (inner join)");
        assertEquals("Produto A", resultado.get(0)[0]);
        assertEquals(2L, resultado.get(0)[1], "só a venda de dentro da janela conta");
    }

    @Test
    @DisplayName("Banco vazio deve devolver lista vazia, nunca null")
    void bancoVazioDevolveListaVazia() {
        assertTrue(produtoRepository.produtosMaisVendidos(10).isEmpty());
        assertTrue(
                produtoRepository
                        .produtosMaisVendidosPorPeriodo(10, agora.minusDays(30), agora)
                        .isEmpty());
    }

    private Produto produto(String nome, String preco) {
        return produtoRepository.save(
                Produto.builder()
                        .nome(nome)
                        .categoria("Pizza")
                        .descricao("Descrição de teste")
                        .preco(new BigDecimal(preco))
                        .restaurante(restaurante)
                        .build());
    }

    /** Cria {@code vezes} pedidos com um item do produto (a query conta itens, não unidades). */
    private void vender(Produto produto, LocalDateTime dataPedido, int vezes) {
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
            // Query nativa não dispara flush automático: o seed precisa sair do contexto de
            // persistência antes da consulta
            pedidoRepository.saveAndFlush(pedido);
        }
    }
}
