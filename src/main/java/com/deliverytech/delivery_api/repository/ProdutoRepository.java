package com.deliverytech.delivery_api.repository;

import com.deliverytech.delivery_api.model.Produto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByNome(String nome);

    List<Produto> findByRestauranteId(Long restauranteId);

    List<Produto> findByDisponivelTrue();

    List<Produto> findByCategoria(String categoria);

    @Query("SELECT p FROM Produto p WHERE LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Produto> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    /**
     * Ranking de produtos mais vendidos (sem filtro de período). Colunas: [0] nome, [1]
     * quantidade_vendida (Long), [2] total_arrecadado (BigDecimal — COALESCE para não virar null
     * quando o produto nunca vendeu).
     */
    @Query(
            value =
                    "SELECT p.nome, COUNT(ip.produto_id) as quantidade_vendida, "
                            + "COALESCE(SUM(ip.subtotal), 0) as total_arrecadado "
                            + "FROM produto p "
                            + "LEFT JOIN item_pedido ip ON p.id = ip.produto_id "
                            + "GROUP BY p.id, p.nome "
                            + "ORDER BY quantidade_vendida DESC, p.nome ASC "
                            + "LIMIT :limite",
            nativeQuery = true)
    List<Object[]> produtosMaisVendidos(@Param("limite") int limite);

    /**
     * Ranking de produtos mais vendidos dentro de uma janela [inicio, fim). Semântica de inner
     * join: produto sem venda no período não aparece. Mesmas colunas de {@link
     * #produtosMaisVendidos(int)}.
     */
    @Query(
            value =
                    "SELECT p.nome, COUNT(ip.produto_id) as quantidade_vendida, "
                            + "COALESCE(SUM(ip.subtotal), 0) as total_arrecadado "
                            + "FROM produto p "
                            + "LEFT JOIN item_pedido ip ON p.id = ip.produto_id "
                            + "LEFT JOIN pedido ped ON ip.pedido_id = ped.id "
                            + "WHERE ped.data_pedido >= :inicio AND ped.data_pedido < :fim "
                            + "GROUP BY p.id, p.nome "
                            + "ORDER BY quantidade_vendida DESC, p.nome ASC "
                            + "LIMIT :limite",
            nativeQuery = true)
    List<Object[]> produtosMaisVendidosPorPeriodo(
            @Param("limite") int limite,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
