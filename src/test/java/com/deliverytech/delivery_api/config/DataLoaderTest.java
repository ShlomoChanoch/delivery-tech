package com.deliverytech.delivery_api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliverytech.delivery_api.model.Cliente;
import com.deliverytech.delivery_api.model.ItemPedido;
import com.deliverytech.delivery_api.model.Pedido;
import com.deliverytech.delivery_api.model.Produto;
import com.deliverytech.delivery_api.model.Restaurante;
import com.deliverytech.delivery_api.model.StatusPedido;
import com.deliverytech.delivery_api.repository.ClienteRepository;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Regressões do seed de pedidos introduzido junto dos relatórios reais. */
@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private RestauranteRepository restauranteRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private PedidoRepository pedidoRepository;

    private DataLoader loader;

    @BeforeEach
    void setUp() {
        loader =
                new DataLoader(
                        clienteRepository,
                        restauranteRepository,
                        produtoRepository,
                        pedidoRepository);
    }

    @Test
    void devePularRestauranteSemCardapioEContinuarComOProximo() throws Exception {
        when(pedidoRepository.count()).thenReturn(0L);
        // O plano referencia o restaurante de índice 1 com o cliente de índice 2
        // (seed "2,1,9"); os índices 0 e 2 ficam sem cardápio e devem ser pulados.
        Restaurante semCardapio = restaurante(1L, "Sem Cardápio");
        Restaurante comCardapio = restaurante(2L, "Com Cardápio");
        Restaurante outroSemCardapio = restaurante(3L, "Outro Sem Cardápio");
        Produto produto = produto(1L, comCardapio, new BigDecimal("10.00"));
        when(clienteRepository.findAll())
                .thenReturn(List.of(cliente(1L), cliente(2L), cliente(3L)));
        when(restauranteRepository.findAll())
                .thenReturn(List.of(semCardapio, comCardapio, outroSemCardapio));
        when(produtoRepository.findAll()).thenReturn(List.of(produto));

        loader.run();

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, atLeastOnce()).save(captor.capture());
        Pedido pedido = captor.getAllValues().get(0);
        assertEquals(comCardapio.getId(), pedido.getRestaurante().getId());
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        assertEquals(new BigDecimal("20.00"), pedido.getValorTotal());
        assertEquals(2, pedido.getItens().size());
        for (ItemPedido item : pedido.getItens()) {
            assertEquals(pedido, item.getPedido());
            assertEquals(new BigDecimal("10.00"), item.getSubtotal());
        }
    }

    @Test
    void deveUsarOMesmoFusoDosModelosAoGerarDatas() throws Exception {
        when(pedidoRepository.count()).thenReturn(0L);
        Cliente cliente = cliente(1L);
        Restaurante restaurante = restaurante(1L, "Restaurante");
        Produto produto = produto(1L, restaurante, new BigDecimal("10.00"));
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(restauranteRepository.findAll()).thenReturn(List.of(restaurante));
        when(produtoRepository.findAll()).thenReturn(List.of(produto));

        LocalDateTime antes = LocalDateTime.now(ZoneId.systemDefault());
        loader.run();
        LocalDateTime depois = LocalDateTime.now(ZoneId.systemDefault());

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, atLeastOnce()).save(captor.capture());
        LocalDateTime data = captor.getAllValues().get(0).getDataPedido();
        // O primeiro pedido do plano fica dois dias no passado e é deslocado 17 minutos.
        assertTrue(data.isAfter(antes.minusDays(2).minusMinutes(18)));
        assertTrue(data.isBefore(depois.minusDays(2).minusMinutes(16)));
    }

    @Test
    void naoDeveInserirPedidosQuandoJaExistem() throws Exception {
        when(pedidoRepository.count()).thenReturn(1L);

        loader.run();

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(clienteRepository, never()).findAll();
    }

    @Test
    void deveIgnorarSeedQuandoFaltamClientesRestaurantesOuProdutos() throws Exception {
        when(pedidoRepository.count()).thenReturn(0L);
        when(clienteRepository.findAll()).thenReturn(List.of());
        when(restauranteRepository.findAll()).thenReturn(List.of());
        when(produtoRepository.findAll()).thenReturn(List.of());

        loader.run();

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void planoDeSeedDeveCompararConteudoDosItens() throws Exception {
        // S6218: o record guarda um int[]; equals/hashCode/toString precisam olhar o
        // conteúdo do array, não a identidade. Instanciado via reflection por ser privado.
        Class<?> seedClass =
                Class.forName("com.deliverytech.delivery_api.config.DataLoader$PedidoSeed");
        var ctor =
                seedClass.getDeclaredConstructor(
                        int.class, int.class, int.class, StatusPedido.class, int[].class);
        ctor.setAccessible(true);
        Object a = ctor.newInstance(0, 1, 9, StatusPedido.ENTREGUE, new int[] {0, 1, 1, 1});
        Object b = ctor.newInstance(0, 1, 9, StatusPedido.ENTREGUE, new int[] {0, 1, 1, 1});
        Object diferente = ctor.newInstance(0, 1, 9, StatusPedido.ENTREGUE, new int[] {0, 2});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, diferente);
        assertTrue(a.toString().contains("[0, 1, 1, 1]"));
    }

    private static Cliente cliente(long id) {
        return Cliente.builder()
                .id(id)
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco("Rua Teste, 123")
                .ativo(true)
                .build();
    }

    private static Restaurante restaurante(long id, String nome) {
        return Restaurante.builder().id(id).nome(nome).ativo(true).build();
    }

    private static Produto produto(long id, Restaurante restaurante, BigDecimal preco) {
        return Produto.builder()
                .id(id)
                .nome("Produto")
                .preco(preco)
                .restaurante(restaurante)
                .disponivel(true)
                .build();
    }
}
