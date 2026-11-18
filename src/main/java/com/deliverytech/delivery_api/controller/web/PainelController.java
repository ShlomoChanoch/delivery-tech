package com.deliverytech.delivery_api.controller.web;

import com.deliverytech.delivery_api.model.Cliente;
import com.deliverytech.delivery_api.model.Pedido;
import com.deliverytech.delivery_api.model.Produto;
import com.deliverytech.delivery_api.model.StatusPedido;
import com.deliverytech.delivery_api.service.ClienteService;
import com.deliverytech.delivery_api.service.PedidoService;
import com.deliverytech.delivery_api.service.ProdutoService;
import com.deliverytech.delivery_api.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller do painel web público (sem autenticação). Consome os mesmos services da API REST — não
 * há chamada HTTP interna.
 */
@Slf4j
@Controller
@RequestMapping("/painel")
@RequiredArgsConstructor
public class PainelController {

    private static final int LIMITE_RELATORIO = 10;
    private static final int DIAS_DASHBOARD = 30;

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final RelatorioService relatorioService;

    /** Dashboard com resumo de vendas e pedidos do período GET /painel */
    @GetMapping
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusDays(DIAS_DASHBOARD);

        Map<String, Object> resumo = relatorioService.resumoVendas(null, null);
        Map<String, Object> pedidosPeriodo =
                relatorioService.relatorioPedidosPorPeriodo(inicio, hoje, "dia");

        log.debug("Dashboard montado - resumo: {}, periodo: {}", resumo, pedidosPeriodo);

        model.addAttribute("resumo", resumo);
        model.addAttribute("pedidosPeriodo", pedidosPeriodo);
        model.addAttribute("periodoDias", DIAS_DASHBOARD);
        model.addAttribute("periodoInicio", inicio);
        model.addAttribute("periodoFim", hoje);
        model.addAttribute(
                "vendasPorRestaurante", relatorioService.relatorioVendasPorRestaurante(null, null));
        model.addAttribute(
                "produtosMaisVendidos",
                relatorioService.relatorioProdutosMaisVendidos(LIMITE_RELATORIO, null, null));
        model.addAttribute(
                "clientesMaisAtivos",
                relatorioService.relatorioClientesAtivos(LIMITE_RELATORIO, null, null));
        return "painel/dashboard";
    }

    /** Lista clientes ativos ou busca por nome GET /painel/clientes?q=ana */
    @GetMapping("/clientes")
    @Transactional(readOnly = true)
    public String clientes(@RequestParam(required = false) String q, Model model) {
        String termo = q == null ? "" : q.trim();
        List<Cliente> clientes =
                termo.isEmpty()
                        ? clienteService.listarAtivos()
                        : clienteService.buscarPorNome(termo);

        log.debug("Painel de clientes - termo: '{}', resultados: {}", termo, clientes.size());

        model.addAttribute("clientes", clientes);
        model.addAttribute("q", termo);
        return "painel/clientes";
    }

    /** Lista produtos, com filtro opcional por categoria GET /painel/produtos?categoria=Pizza */
    @GetMapping("/produtos")
    @Transactional(readOnly = true)
    public String produtos(@RequestParam(required = false) String categoria, Model model) {
        String filtro = categoria == null ? "" : categoria.trim();
        List<Produto> produtos =
                filtro.isEmpty()
                        ? produtoService.listarTodos()
                        : produtoService.buscarPorCategoria(filtro);
        List<String> categorias =
                produtoService.listarTodos().stream()
                        .map(Produto::getCategoria)
                        .filter(c -> c != null && !c.isBlank())
                        .distinct()
                        .sorted()
                        .toList();

        log.debug("Painel de produtos - filtro: '{}', resultados: {}", filtro, produtos.size());

        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoria", filtro);
        return "painel/produtos";
    }

    /**
     * Lista pedidos com filtro opcional de status GET /painel/pedidos?status=CRIADO
     *
     * <p>Não acessa Pedido.itens (coleção LAZY com OSIV desabilitado) — apenas associações EAGER e
     * colunas simples.
     */
    @GetMapping("/pedidos")
    @Transactional(readOnly = true)
    public String pedidos(@RequestParam(required = false) String status, Model model) {
        StatusPedido statusEnum = parseStatus(status);
        List<Pedido> pedidos = pedidoService.listarComFiltros(statusEnum, null, null);

        log.debug("Painel de pedidos - status: {}, resultados: {}", statusEnum, pedidos.size());

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("statusValues", StatusPedido.values());
        model.addAttribute("status", statusEnum == null ? "" : statusEnum.name());
        return "painel/pedidos";
    }

    /** Relatórios gerenciais (3 blocos) GET /painel/relatorios */
    @GetMapping("/relatorios")
    @Transactional(readOnly = true)
    public String relatorios(Model model) {
        List<Map<String, Object>> vendasPorRestaurante =
                relatorioService.relatorioVendasPorRestaurante(null, null);
        List<Map<String, Object>> produtosMaisVendidos =
                relatorioService.relatorioProdutosMaisVendidos(LIMITE_RELATORIO, null, null);
        List<Map<String, Object>> clientesAtivos =
                relatorioService.relatorioClientesAtivos(LIMITE_RELATORIO, null, null);

        model.addAttribute("vendasPorRestaurante", vendasPorRestaurante);
        model.addAttribute("produtosMaisVendidos", produtosMaisVendidos);
        model.addAttribute("clientesAtivos", clientesAtivos);
        model.addAttribute("limite", LIMITE_RELATORIO);
        return "painel/relatorios";
    }

    /** Converte o parâmetro de status em enum, ignorando valores vazios ou desconhecidos */
    private StatusPedido parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusPedido.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Status desconhecido ignorado no painel: '{}'", status);
            return null;
        }
    }
}
