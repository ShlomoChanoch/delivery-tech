package com.deliverytech.delivery_api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.deliverytech.delivery_api.repository.ClienteRepository;
import com.deliverytech.delivery_api.repository.PedidoRepository;
import com.deliverytech.delivery_api.repository.ProdutoRepository;
import com.deliverytech.delivery_api.repository.RestauranteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class DataLoaderIT {

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private RestauranteRepository restauranteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private PedidoRepository pedidoRepository;

    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        restauranteRepository.deleteAll();
        clienteRepository.deleteAll();

        dataLoader =
                new DataLoader(
                        clienteRepository,
                        restauranteRepository,
                        produtoRepository,
                        pedidoRepository);
    }

    @Test
    @DisplayName("Executar DataLoader duas vezes não deve duplicar registros nem lançar erro")
    void executarDuasVezes_DeveSerIdempotente() throws Exception {
        dataLoader.run();

        assertEquals(5, clienteRepository.count());
        assertEquals(5, restauranteRepository.count());
        assertEquals(10, produtoRepository.count());
        assertEquals(12, pedidoRepository.count());

        dataLoader.run();

        assertEquals(5, clienteRepository.count());
        assertEquals(5, restauranteRepository.count());
        assertEquals(10, produtoRepository.count());
        assertEquals(12, pedidoRepository.count());
    }
}
