package com.deliverytech.delivery_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.deliverytech.delivery_api.model.Role;
import com.deliverytech.delivery_api.model.Usuario;
import com.deliverytech.delivery_api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unitários do JwtAuthenticationFilter (Fase 2 / T4): foco no null-guard de usuário deletado —
 * token ainda válido, mas findByEmail devolve null. Antes do fix isso era NPE dentro do filtro (500
 * em qualquer rota); agora a requisição segue sem autenticação e o Security barra com 401/403.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("juju@email.com");
        u.setRole(Role.USER);
        u.setAtivo(true);
        return u;
    }

    @Test
    @DisplayName("Sem header Authorization: segue a cadeia sem autenticação")
    void semHeader_SegueSemAutenticacao() throws Exception {
        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Token válido e usuário existente: autentica com as authorities do usuário")
    void tokenValido_UsuarioExiste_Autentica() throws Exception {
        Usuario u = usuario();
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtUtil.getEmailFromToken("token-valido")).thenReturn(u.getEmail());
        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(u);

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertSame(u, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(Role.USER));
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Token válido de usuário deletado: não autentica, não lança NPE e segue a cadeia")
    void tokenValido_UsuarioDeletado_NaoAutenticaSemNpe() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtUtil.getEmailFromToken("token-valido")).thenReturn("deletado@email.com");
        when(usuarioRepository.findByEmail("deletado@email.com")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }
}
