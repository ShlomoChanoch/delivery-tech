package com.deliverytech.delivery_api.repository;

import com.deliverytech.delivery_api.model.Restaurante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    boolean existsByNome(String nome);

    List<Restaurante> findByCategoria(String categoria);

    List<Restaurante> findByAtivoTrue();

    long countByAtivoTrue();

    List<Restaurante> findByAvaliacaoGreaterThanEqual(BigDecimal avaliacao);

    List<Restaurante> findByTaxaEntregaLessThanEqual(BigDecimal taxa);

    /**
     * Vendas por restaurante (sem filtro de período), ordenado do maior para o menor nº de pedidos
     * — o dashboard usa a linha [0] como máximo das barras.
     */
    @Query(
            "SELECT r.nome as nomeRestaurante, "
                    + "SUM(p.valorTotal) as totalVendas, "
                    + "COUNT(p.id) as quantidadePedidos "
                    + "FROM Restaurante r "
                    + "LEFT JOIN Pedido p ON r.id = p.restaurante.id "
                    + "GROUP BY r.id, r.nome "
                    + "ORDER BY COUNT(p.id) DESC, r.nome ASC")
    List<RelatorioVendas> relatorioVendasPorRestaurante();

    /**
     * Vendas por restaurante dentro de uma janela [inicio, fim). Semântica de inner join:
     * restaurante sem pedido no período não aparece.
     */
    @Query(
            "SELECT r.nome as nomeRestaurante, "
                    + "SUM(p.valorTotal) as totalVendas, "
                    + "COUNT(p.id) as quantidadePedidos "
                    + "FROM Restaurante r "
                    + "LEFT JOIN Pedido p ON r.id = p.restaurante.id "
                    + "WHERE p.dataPedido >= :inicio AND p.dataPedido < :fim "
                    + "GROUP BY r.id, r.nome "
                    + "ORDER BY COUNT(p.id) DESC, r.nome ASC")
    List<RelatorioVendas> relatorioVendasPorRestaurantePorPeriodo(
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    List<Restaurante> findByAtivoFalse();

    List<Restaurante> findByCategoriaAndAtivo(String categoria, Boolean ativo);
}
