package com.deliverytech.delivery_api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliverytech.delivery_api.dto.request.LoginRequest;
import com.deliverytech.delivery_api.dto.request.RegisterRequest;
import com.deliverytech.delivery_api.dto.response.LoginResponse;
import com.deliverytech.delivery_api.model.Role;
import com.deliverytech.delivery_api.model.Usuario;
import com.deliverytech.delivery_api.repository.UsuarioRepository;
import com.deliverytech.delivery_api.security.JwtUtil;
import com.deliverytech.delivery_api.service.impl.UsuarioServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unitários do UsuarioServiceImpl (Fase 2 / T4): o caminho de auth real vive aqui — login via
 * AuthenticationManager (mockado: o provider real só existe em IT) e loadUserByUsername, movido do
 * AuthServiceImpl removido. O fluxo HTTP completo (register/login/token) é coberto pelos ITs.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioServiceImpl service;

    private static final String EMAIL = "juju@email.com";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNome("Juju");
        u.setEmail(EMAIL);
        u.setSenha("hash-bcrypt");
        u.setRole(Role.USER);
        u.setAtivo(true);
        return u;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setUsername(EMAIL);
        r.setPassword("senha-forte");
        return r;
    }

    private RegisterRequest registerRequest(Role role) {
        RegisterRequest r = new RegisterRequest();
        r.setNome("Juju");
        r.setEmail(EMAIL);
        r.setSenha("senha-forte");
        r.setRole(role);
        return r;
    }

    // === login ===

    @Test
    @DisplayName("Login: credenciais válidas geram token e resposta de sucesso")
    void login_CredenciaisValidas_RetornaToken() {
        var autenticado = new UsernamePasswordAuthenticationToken(EMAIL, null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(autenticado);
        when(jwtUtil.gerarToken(EMAIL)).thenReturn("jwt-de-tres-partes");

        LoginResponse response = service.login(loginRequest());

        assertEquals("jwt-de-tres-partes", response.getToken());
        assertEquals(EMAIL, response.getUsername());
        assertEquals("Login realizado com sucesso", response.getMessage());
    }

    @Test
    @DisplayName("Login: credenciais inválidas viram RuntimeException com a causa preservada")
    void login_CredenciaisInvalidas_LancaRuntimeException() {
        var causa = new BadCredentialsException("Bad credentials");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(causa);
        LoginRequest request = loginRequest();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.login(request));

        assertEquals("Credenciais inválidas", ex.getMessage());
        assertSame(causa, ex.getCause());
        verify(jwtUtil, never()).gerarToken(any());
    }

    // === loadUserByUsername (movido do AuthServiceImpl removido) ===

    @Test
    @DisplayName("loadUserByUsername: email existente devolve o próprio Usuario como UserDetails")
    void loadUserByUsername_UsuarioExiste_RetornaUserDetails() {
        Usuario u = usuario();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(u);

        UserDetails resultado = service.loadUserByUsername(EMAIL);

        assertSame(u, resultado);
        assertEquals(EMAIL, resultado.getUsername());
        assertTrue(resultado.getAuthorities().contains(Role.USER));
    }

    @Test
    @DisplayName("loadUserByUsername: email inexistente lança UsernameNotFoundException")
    void loadUserByUsername_UsuarioInexistente_LancaUsernameNotFoundException() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(null);

        UsernameNotFoundException ex =
                assertThrows(
                        UsernameNotFoundException.class, () -> service.loadUserByUsername(EMAIL));

        assertTrue(ex.getMessage().contains(EMAIL));
    }

    // === salvar (register) ===

    @Test
    @DisplayName("Salvar: sem role informada assume USER, codifica a senha e ativa o usuário")
    void salvar_SemRole_AssumeUserECodificaSenha() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode("senha-forte")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario salvo = service.salvar(registerRequest(null));

        assertEquals(Role.USER, salvo.getRole());
        assertEquals("hash-bcrypt", salvo.getSenha());
        assertTrue(salvo.getAtivo());
        assertNotNull(salvo.getDataCriacao());
        verify(usuarioRepository).save(salvo);
    }

    @Test
    @DisplayName("Salvar: role informada é respeitada")
    void salvar_ComRole_RespeitaRoleInformada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode("senha-forte")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario salvo = service.salvar(registerRequest(Role.ADMIN));

        assertEquals(Role.ADMIN, salvo.getRole());
    }

    @Test
    @DisplayName("Salvar: email já cadastrado lança exceção e não persiste")
    void salvar_EmailDuplicado_LancaExcecao() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(usuario());
        RegisterRequest request = registerRequest(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertTrue(ex.getMessage().contains(EMAIL));
        verify(usuarioRepository, never()).save(any());
    }

    // === consultas ===

    @Test
    @DisplayName("buscarPorEmail: existente devolve UserDetails")
    void buscarPorEmail_Existe_RetornaUserDetails() {
        Usuario u = usuario();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(u);

        assertSame(u, service.buscarPorEmail(EMAIL));
    }

    @Test
    @DisplayName("buscarPorEmail: inexistente lança RuntimeException")
    void buscarPorEmail_Inexistente_LancaExcecao() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.buscarPorEmail(EMAIL));
    }

    @Test
    @DisplayName("existePorEmail: reflete a presença no repositório")
    void existePorEmail_RefleteRepositorio() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(usuario());
        assertTrue(service.existePorEmail(EMAIL));

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(null);
        assertFalse(service.existePorEmail(EMAIL));
    }

    @Test
    @DisplayName("buscarPorId: existente devolve o usuário")
    void buscarPorId_Existe_RetornaUsuario() {
        Usuario u = usuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertSame(u, service.buscarPorId(1L));
    }

    @Test
    @DisplayName("buscarPorId: inexistente lança RuntimeException")
    void buscarPorId_Inexistente_LancaExcecao() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buscarPorId(1L));
    }

    // === inativar ===

    @Test
    @DisplayName("inativarUsuario: existente marca inativo e salva")
    void inativarUsuario_Existe_MarcaInativoESalva() {
        Usuario u = usuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        service.inativarUsuario(1L);

        assertFalse(u.getAtivo());
        verify(usuarioRepository).save(u);
    }

    @Test
    @DisplayName("inativarUsuario: inexistente lança RuntimeException sem salvar")
    void inativarUsuario_Inexistente_LancaExcecao() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.inativarUsuario(1L));
        verify(usuarioRepository, never()).save(any());
    }
}
