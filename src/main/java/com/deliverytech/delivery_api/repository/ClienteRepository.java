package com.deliverytech.delivery_api.repository;

import com.deliverytech.delivery_api.model.Cliente;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Cliente> findByAtivoTrue();

    long countByAtivoTrue();

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    /**
     * Ranking de clientes por pedidos, apenas ativos (sem filtro de período). Colunas: [0] nome,
     * [1] total_pedidos (Long), [2] total_gasto (BigDecimal — COALESCE para não virar null quando o
     * cliente não tem pedidos).
     */
    @Query(
            value =
                    "SELECT c.nome, COUNT(p.id) as total_pedidos, "
                            + "COALESCE(SUM(p.valor_total), 0) as total_gasto "
                            + "FROM cliente c "
                            + "LEFT JOIN pedido p ON c.id = p.cliente_id "
                            + "WHERE c.ativo = TRUE "
                            + "GROUP BY c.id, c.nome "
                            + "ORDER BY total_pedidos DESC, c.nome ASC "
                            + "LIMIT :limite",
            nativeQuery = true)
    List<Object[]> rankingClientesPorPedidos(@Param("limite") int limite);

    /**
     * Ranking de clientes por pedidos dentro de uma janela [inicio, fim), apenas ativos. Semântica
     * de inner join: cliente sem pedido no período não aparece. Mesmas colunas de {@link
     * #rankingClientesPorPedidos(int)}.
     */
    @Query(
            value =
                    "SELECT c.nome, COUNT(p.id) as total_pedidos, "
                            + "COALESCE(SUM(p.valor_total), 0) as total_gasto "
                            + "FROM cliente c "
                            + "LEFT JOIN pedido p ON c.id = p.cliente_id "
                            + "WHERE c.ativo = TRUE "
                            + "AND p.data_pedido >= :inicio AND p.data_pedido < :fim "
                            + "GROUP BY c.id, c.nome "
                            + "ORDER BY total_pedidos DESC, c.nome ASC "
                            + "LIMIT :limite",
            nativeQuery = true)
    List<Object[]> rankingClientesPorPedidosPorPeriodo(
            @Param("limite") int limite,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
