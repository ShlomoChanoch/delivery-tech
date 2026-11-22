package com.deliverytech.delivery_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class RelatorioControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ClienteRepository clienteRepository;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();
        clienteRepository.deleteAll();

        Restaurante restaurante =
                restauranteRepository.save(
                        Restaurante.builder()
                                .nome("Pizzaria Relatório")
                                .categoria("Pizza")
                                .taxaEntrega(BigDecimal.ZERO)
                                .ativo(true)
                                .build());

        Cliente cliente =
                clienteRepository.save(
                        Cliente.builder()
                                .nome("Ana Relatório")
                                .email("ana.relatorio@teste.com")
                                .ativo(true)
                                .build());

        Produto produto =
                produtoRepository.save(
                        Produto.builder()
                                .nome("Calabresa Especial")
                                .categoria("Pizza")
                                .preco(new BigDecimal("40.00"))
                                .disponivel(true)
                                .restaurante(restaurante)
                                .build());

        LocalDateTime agora = LocalDateTime.now(ZoneId.systemDefault());
        for (int i = 0; i < 2; i++) {
            Pedido pedido =
                    Pedido.builder()
                            .cliente(cliente)
                            .restaurante(restaurante)
                            .status(StatusPedido.ENTREGUE)
                            .dataPedido(agora.minusDays(i + 1L))
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

    @Test
    @DisplayName("GET /api/relatorios/* sem autenticação deve retornar 403")
    void relatoriosSemAuth_DeveRetornar403() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo-vendas")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/relatorios/vendas-por-restaurante - deve retornar ranking real")
    @WithMockUser
    void vendasPorRestaurante_DeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/relatorios/vendas-por-restaurante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurante").value("Pizzaria Relatório"))
                .andExpect(jsonPath("$[0].quantidadePedidos").value(2));
    }

    @Test
    @DisplayName("GET /api/relatorios/produtos-mais-vendidos e /clientes-ativos - 200 com dados")
    @WithMockUser
    void produtosMaisVendidosEClientesAtivos_DeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/relatorios/produtos-mais-vendidos").param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Calabresa Especial"))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(2));

        mockMvc.perform(get("/api/relatorios/clientes-ativos").param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente").value("Ana Relatório"))
                .andExpect(jsonPath("$[0].quantidadePedidos").value(2));
    }

    @Test
    @DisplayName("GET /api/relatorios/pedidos-por-periodo e /resumo-vendas - 200 com agregados")
    @WithMockUser
    void pedidosPorPeriodoEResumoVendas_DeveRetornar200() throws Exception {
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        LocalDate inicio = hoje.minusDays(7);

        mockMvc.perform(
                        get("/api/relatorios/pedidos-por-periodo")
                                .param("dataInicio", inicio.toString())
                                .param("dataFim", hoje.toString())
                                .param("agrupamento", "dia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(2));

        mockMvc.perform(get("/api/relatorios/resumo-vendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(2))
                .andExpect(jsonPath("$.clientesAtivos").value(1))
                .andExpect(jsonPath("$.restaurantesAtivos").value(1));
    }
}
