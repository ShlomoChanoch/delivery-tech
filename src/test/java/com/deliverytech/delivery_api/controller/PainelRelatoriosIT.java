package com.deliverytech.delivery_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponta a ponta dos relatórios do painel: seed nos repositories → queries reais → {@link
 * com.deliverytech.delivery_api.service.impl.RelatorioServiceImpl} → templates Thymeleaf. Pega erro
 * de chave de map inexistente no template, de formatação e de SQL que só falha na execução — nada
 * disso aparece em teste unitário do service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class PainelRelatoriosIT {

    @Autowired private MockMvc mockMvc;

    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();
        clienteRepository.deleteAll();
        semear();
    }

    @Test
    @DisplayName("GET /painel/relatorios - 200 com rankings dos dados semeadados")
    void relatorios_ComDadosSemeadados() throws Exception {
        mockMvc.perform(get("/painel/relatorios"))
                .andExpect(status().isOk())
                // ranking de restaurantes (o alias corrigido é o que permite estes valores)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rest Teste")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("Produto Campeão")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente Top")));
    }

    @Test
    @DisplayName("GET /painel - dashboard 200 com KPIs dos dados semeadados")
    void dashboard_ComDadosSemeadados() throws Exception {
        mockMvc.perform(get("/painel"))
                .andExpect(status().isOk())
                // 2 pedidos de R$ 30,00 → ticket médio 30,00
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30,00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rest Teste")));
    }

    @Test
    @DisplayName("GET /painel/relatorios - 200 com banco vazio (sem dados não pode quebrar)")
    void relatorios_BancoVazio() throws Exception {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();
        clienteRepository.deleteAll();

        mockMvc.perform(get("/painel/relatorios")).andExpect(status().isOk());
        mockMvc.perform(get("/painel")).andExpect(status().isOk());
    }

    /** Seed mínimo e determinístico: 1 restaurante, 1 cliente, 1 produto, 2 pedidos. */
    private void semear() {
        Restaurante restaurante =
                restauranteRepository.save(
                        Restaurante.builder()
                                .nome("Rest Teste")
                                .categoria("Pizza")
                                .avaliacao(new BigDecimal("4.5"))
                                .taxaEntrega(BigDecimal.ZERO)
                                .ativo(true)
                                .build());
        Cliente cliente =
                clienteRepository.save(
                        Cliente.builder()
                                .nome("Cliente Top")
                                .email("top@teste.com")
                                .ativo(true)
                                .build());
        Produto produto =
                produtoRepository.save(
                        Produto.builder()
                                .nome("Produto Campeão")
                                .categoria("Pizza")
                                .preco(new BigDecimal("30.00"))
                                .restaurante(restaurante)
                                .build());

        for (int i = 0; i < 2; i++) {
            Pedido pedido =
                    Pedido.builder()
                            .cliente(cliente)
                            .restaurante(restaurante)
                            .status(StatusPedido.ENTREGUE)
                            .dataPedido(LocalDateTime.now().minusDays(i + 1))
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
