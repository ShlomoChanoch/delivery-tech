package com.deliverytech.delivery_api.service.impl;

import com.deliverytech.delivery_api.repository.ClienteRepository;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RelatorioVendas;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import com.deliverytech.delivery_api.service.RelatorioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios gerenciais com dados REAIS dos repositories. As chaves dos Maps retornados são o
 * contrato dos templates do painel (`dashboard.html` e `relatorios.html`) — rename de chave quebra
 * o painel silenciosamente; mantenha sincronizado com os testes de contrato em {@code
 * RelatorioServiceImplTest}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioServiceImpl implements RelatorioService {

    /**
     * Bordo ausente na normalização do período: só entra em jogo quando o chamador informa apenas
     * uma das datas (a API permite). Evita {@code LocalDate.now()} sem zona (regra java:S8688).
     */
    private static final LocalDate LIMITE_INFERIOR = LocalDate.of(1970, 1, 1);

    private static final LocalDate LIMITE_SUPERIOR = LocalDate.of(2999, 12, 31);

    private static final String AGRUPAMENTO_PADRAO = "total";

    private final RestauranteRepository restauranteRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    public List<Map<String, Object>> relatorioVendasPorRestaurante(
            LocalDate dataInicio, LocalDate dataFim) {
        List<RelatorioVendas> linhas =
                isSemPeriodo(dataInicio, dataFim)
                        ? restauranteRepository.relatorioVendasPorRestaurante()
                        : restauranteRepository.relatorioVendasPorRestaurantePorPeriodo(
                                inicio(dataInicio), fim(dataFim));

        List<Map<String, Object>> resultado = new ArrayList<>(linhas.size());
        for (RelatorioVendas linha : linhas) {
            resultado.add(
                    Map.of(
                            "restaurante",
                            linha.getNomeRestaurante() == null ? "" : linha.getNomeRestaurante(),
                            "quantidadePedidos",
                            linha.getQuantidadePedidos() == null
                                    ? 0L
                                    : linha.getQuantidadePedidos(),
                            "totalVendas",
                            linha.getTotalVendas() == null
                                    ? BigDecimal.ZERO
                                    : linha.getTotalVendas()));
        }
        log.debug(
                "Vendas por restaurante: {} linha(s), periodo={}",
                resultado.size(),
                isSemPeriodo(dataInicio, dataFim) ? "geral" : dataInicio + ".." + dataFim);
        return resultado;
    }

    @Override
    public List<Map<String, Object>> relatorioProdutosMaisVendidos(
            int limite, LocalDate dataInicio, LocalDate dataFim) {
        List<Object[]> linhas =
                isSemPeriodo(dataInicio, dataFim)
                        ? produtoRepository.produtosMaisVendidos(limite)
                        : produtoRepository.produtosMaisVendidosPorPeriodo(
                                limite, inicio(dataInicio), fim(dataFim));

        List<Map<String, Object>> resultado = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            resultado.add(
                    Map.of(
                            "produto", asTexto(linha[0]),
                            "quantidadeVendida", asLong(linha[1]),
                            "totalArrecadado", asDecimal(linha[2])));
        }
        return resultado;
    }

    @Override
    public List<Map<String, Object>> relatorioClientesAtivos(
            int limite, LocalDate dataInicio, LocalDate dataFim) {
        List<Object[]> linhas =
                isSemPeriodo(dataInicio, dataFim)
                        ? clienteRepository.rankingClientesPorPedidos(limite)
                        : clienteRepository.rankingClientesPorPedidosPorPeriodo(
                                limite, inicio(dataInicio), fim(dataFim));

        List<Map<String, Object>> resultado = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            resultado.add(
                    Map.of(
                            "cliente", asTexto(linha[0]),
                            "quantidadePedidos", asLong(linha[1]),
                            "totalGasto", asDecimal(linha[2])));
        }
        return resultado;
    }

    @Override
    public Map<String, Object> relatorioPedidosPorPeriodo(
            LocalDate dataInicio, LocalDate dataFim, String agrupamento) {
        // A API exige as datas e o painel sempre passa a janela de 30 dias; a normalização
        // cobre o caso degenerado de ambos nulos (vira a janela inteira).
        Object[] agregado =
                pedidoRepository.contarVendasPorPeriodo(inicio(dataInicio), fim(dataFim));
        long totalPedidos = asLong(agregado[0]);

        return Map.of(
                "totalPedidos",
                totalPedidos,
                "valorTotal",
                asDecimal(agregado[1]),
                "agrupamento",
                agrupamento == null || agrupamento.isBlank() ? AGRUPAMENTO_PADRAO : agrupamento);
    }

    @Override
    public Map<String, Object> resumoVendas(LocalDate dataInicio, LocalDate dataFim) {
        Object[] agregado =
                isSemPeriodo(dataInicio, dataFim)
                        ? pedidoRepository.contarVendasGeral()
                        : pedidoRepository.contarVendasPorPeriodo(inicio(dataInicio), fim(dataFim));
        long totalPedidos = asLong(agregado[0]);
        BigDecimal valorTotalVendas = asDecimal(agregado[1]);
        BigDecimal ticketMedio =
                totalPedidos > 0
                        ? valorTotalVendas.divide(
                                BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        return Map.of(
                "totalPedidos", totalPedidos,
                "valorTotalVendas", valorTotalVendas,
                "ticketMedio", ticketMedio,
                "restaurantesAtivos", restauranteRepository.countByAtivoTrue(),
                "clientesAtivos", clienteRepository.countByAtivoTrue());
    }

    /** Período ausente de verdade = nenhuma das duas datas informadas. */
    private static boolean isSemPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return dataInicio == null && dataFim == null;
    }

    /** Início inclusivo: {@code data >= inicio.atStartOfDay()}. */
    private static LocalDateTime inicio(LocalDate data) {
        return (data == null ? LIMITE_INFERIOR : data).atStartOfDay();
    }

    /** Fim exclusivo: {@code data < (fim + 1 dia).atStartOfDay()} — cobre o dia inteiro. */
    private static LocalDateTime fim(LocalDate data) {
        return (data == null ? LIMITE_SUPERIOR : data).plusDays(1).atStartOfDay();
    }

    private static String asTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private static long asLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private static BigDecimal asDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal decimal ? decimal : new BigDecimal(valor.toString());
    }
}
