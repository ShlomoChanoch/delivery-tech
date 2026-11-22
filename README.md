# 🚀 Delivery API

API backend desenvolvida em **Java 21** com **Spring Boot 3.5.14**, como base para um sistema de delivery moderno, escalável e de fácil manutenção — agora com **painel web público** (Thymeleaf) sobre os mesmos services.

> **Status atual:** ✅ `make test` verde (135 testes, 0 falhas) · `make lint` verde · `make scan` com `.trivyignore` documentado · ✅ painel `/painel` com 5 páginas validado por Playwright (0 erros de console) · ✅ SonarQube Quality Gate OK · 🔐 `jwt.secret` via variável de ambiente `JWT_SECRET` (nunca hardcoded).

---

## 📌 Visão Geral

Este projeto tem como objetivo fornecer uma estrutura inicial robusta para aplicações de delivery, servindo como ponto de partida para funcionalidades como:

- Cadastro e gerenciamento de usuários, clientes, restaurantes e produtos
- Catálogo de produtos com busca, categoria e disponibilidade
- Gestão de pedidos (criação, itens, confirmação, cancelamento e transição de status)
- Relatórios de vendas, produtos mais vendidos e clientes ativos
- Autenticação JWT e documentação OpenAPI/Swagger
- Monitoramento e métricas via Actuator/Prometheus
- Painel web público em Thymeleaf (`/painel`) — dashboard, clientes, produtos, pedidos e relatórios, sem login

---

## 🛠️ Tecnologias e Ferramentas

- **Java 21** – LTS  
- **Spring Boot 3.5.14** – API REST; versões fixadas no `pom.xml` para correção de CVEs: Spring Framework **6.2.19**, Spring Security **6.5.9**, Tomcat **10.1.59**, Jackson **2.21.7**  
- **Spring Security + JWT** – autenticação stateless (BCrypt, filtro `JwtAuthenticationFilter`)  
- **Spring Data JPA + H2** – banco em memória para dev/teste (driver MySQL 8.4 incluído no `pom.xml`)  
- **Caffeine** – cache  
- **SpringDoc OpenAPI** – Swagger UI em `/swagger-ui.html`  
- **Lombok 1.18.48**, **ModelMapper** e **Bean Validation**  
- **Maven** com wrapper (`./mvnw`)  
- **Spotless** (google-java-format, estilo AOSP) – formatação e checagem de código  
- **JaCoCo** – cobertura de testes  
- **Failsafe** – testes de integração executados no `mvn verify`  
- **Docker** – build multi-stage com imagem final **distroless** (sem shell, usuário `nonroot`) + `docker-compose` com Adminer  
- **Trivy** – scan de config, vulnerabilidades e segredos  
- **GitHub Actions** – pipeline de CI/CD  
- **Make** – atalhos para build, testes, lint e scan  

---

## 📈 Status do Projeto

✅ **Funcional e verificado**  
> Aplicação iniciando com sucesso, DataLoader carregando dados de teste, H2 Console ativo e suíte de testes completa verde (104 unitários + 31 de integração).

---

## ✅ Funcionalidades Implementadas

- [x] Estrutura inicial do projeto com Spring Initializr  
- [x] Dependências essenciais configuradas (Web, JPA, H2, DevTools)
- [x] Modelos de domínio (Cliente, Restaurante, Produto, Pedido)
- [x] Repositories com consultas customizadas
- [x] DataLoader para carga de dados de teste
- [x] H2 Console configurado e funcionando
- [x] Consultas SQL customizadas implementadas
- [x] DTOs de request/response e validação com Bean Validation
- [x] Tratamento global de exceções via `@RestControllerAdvice`
- [x] CRUD completo de clientes, restaurantes, produtos e pedidos — **50 endpoints**
- [x] Fluxo de pedidos: criação, itens, confirmação, cancelamento e transição de status
- [x] Autenticação JWT (`POST /api/auth/login` e `/register`) e autorização por filtro
- [x] Documentação OpenAPI/Swagger configurada
- [x] Filtro de captura de requisições/respostas em `entregaveis/`
- [x] Testes unitários e de integração (Surefire + Failsafe)
- [x] Formatação automática (Spotless) e cobertura (JaCoCo)
- [x] Build Docker multi-stage com imagem distroless + `docker-compose`
- [x] Pipeline de CI/CD no GitHub Actions
- [x] Scan de segurança com Trivy (`make scan`)
- [x] Painel web público em Thymeleaf — 5 páginas (`/painel/**`, sem login), fragmentos nativos, Bootstrap 5 por CDN e CSS próprio

---

# 🔧 Ciclo Recente: Qualidade, Segurança e Testes

**Últimos 15 commits** (`0e22e5a` → `99cc720`), agrupados por tema. O objetivo do ciclo foi blindar o build, a suíte de testes e a superfície de segurança da API.

### 1️⃣ Padronização de código — Spotless
| Commit | Mensagem | O que mudou |
|---|---|---|
| `0e22e5a` | `style: aplica google-java-format via Spotless` | Formata todo o código no estilo AOSP |
| `1e88be0` | `style: aplica google-java-format via Spotless` | Segunda passada de formatação |
| `750bf9b` | `build: configura Spotless e silencia warnings do Maven` | Plugin no `pom.xml` + alvos `make lint` / `make format` |
| `25655ca` | `chore: ajusta Makefile (.PHONY, check-trivy)` | Corrige alvos fantasma e cria o pre-requisito do scan |

### 2️⃣ Toolchain e build
| Commit | Mensagem | O que mudou |
|---|---|---|
| `b61fd8d` | `build: atualiza Lombok para 1.18.48 e habilita annotation processing (JDK 23+)` | `lombok 1.18.48` + `<proc>full</proc>` no compilador |
| `4e69657` | `build: habilita Failsafe para testes de integração` | `*IT.java` rodam no `mvn verify` (alvo `make test`) |
| `14168b6` | `fix: removendo :--sun-misc-unsafe-memory-access=allow` | Remove a flag de JVM `--sun-misc-unsafe-memory-access=allow` que quebrava o build |
| `270e421` | `build: remove jvm.config com flags exclusivas de JDK 23+` | O projeto roda em **JDK 21** (CI e Docker); as flags só existem no 23+ |

### 3️⃣ Testes de integração
| Commit | Mensagem | O que mudou |
|---|---|---|
| `fe0e389` | `test: corrige 3 ITs que retornavam 500` | ITs alinhados ao comportamento real da API |
| `49afa56` | `test: alinha ITs de Pedido ao 404 para entidades inexistentes (Grupo A)` | Contrato de erro padronizado |
| `c7f0699` | `test: corrige JSON paths nos ITs de Cliente (Grupo B)` | Asserções corrigidas nos caminhos do JSON |

### 4️⃣ Segurança e dependências
| Commit | Mensagem | O que mudou |
|---|---|---|
| `5878daa` | `fix(deps): atualiza Spring Boot para 3.5.14 e corrige CVEs` | Boot **3.2.x → 3.5.14** + fixos de Spring Framework 6.2.19, Spring Security 6.5.9 e Tomcat 10.1.59 |
| `44fec26` | `build(docker): troca imagem base para distroless Debian 13` | Runtime sem shell e usuário `nonroot` |
| `2a39608` | `ci(make): adiciona alvo scan com Trivy` | `make scan` (config + vulnerabilidades + segredos) e `.trivyignore` |

### 5️⃣ Refatoração
| Commit | Mensagem | O que mudou |
|---|---|---|
| `99cc720` | `refactor: resolve avisos do SonarLint/LSP e corrige stubs de login` | Injeção via construtor, `System.out` → logger, `@Builder.Default`, APIs deprecated, `@MockBean` → `@MockitoBean` e remoção de stubs que lançavam `UnsupportedOperationException` |

**Resultado do ciclo:** `make lint` ✅ · `make test` ✅ (60 testes, 0 falhas) · `make scan` disponível para auditoria de segurança

> **Nota sobre CVEs ignoradas:** o `.trivyignore` contém `CVE-2026-47884` (RCE via `XsltView`) e `CVE-2026-47890` (stream SSE com views). A correção só existe na linha 7.0.x (Spring Boot 4), e **esta API não usa XSLT nem SSE** — é REST/JSON puro —, portanto o risco não se aplica. A justificativa está documentada no próprio arquivo.

---

# 🖥️ Ciclo Recente: Painel Web Thymeleaf

**Commits `3cf3633` → `9817660`** (nov/2026). O painel é **público** (sem login), serve as 5 páginas do domínio sobre os **mesmos services** da API — sem chamada HTTP interna e sem build de frontend.

### 1️⃣ Implementação — `feat`
| Commit | Mensagem | O que mudou |
|---|---|---|
| `3cf3633` | `feat(web): adiciona painel Thymeleaf público com dashboard e relatórios` | `PainelController` (5 rotas), 5 templates + fragmento `fragments/layout.html`, `static/css/painel.css`, Bootstrap 5.3.3 por CDN, `permitAll /painel/**` no `SecurityConfig` |

### 2️⃣ Correções de renderização — `fix`
| Commit | Mensagem | O que mudou |
|---|---|---|
| `b3c216b` | `fix(web): evita request de favicon no painel e quebra de linha em colunas de preço` | Favicon por data-URI no `<head>` (elimina o 403/500 a cada página) + `text-nowrap` em 5 `<td>` de preço |

### 3️⃣ Acessibilidade e superfícies do navegador — `polish`
| Commit | Mensagem | O que mudou |
|---|---|---|
| `9817660` | `polish(web): a11y e superfícies do navegador no painel (lang, main único, foco, contraste)` | `lang="pt-BR"`, `<main>` único (antes havia 2 aninhados), anéis de foco temados, chip ativo **2,80:1 → 5,18:1**, `aria-current`, skip link, hierarquia h1→h2, `tabular-nums`, scrollbar temada, `rel="noopener"`, `dl` do dashboard empilhando no mobile |

### Verificação
- **Playwright:** 5/5 páginas renderizando, **0 erros / 0 warnings** de console, 0 overflow horizontal (1440 / 768 / 375), navegação por clique (sidebar + chips de filtro) OK.
- **Detector da skill `impeccable`:** `[]` (zero findings) · `make lint` ✅ · `make test` ✅ (60/60).

> **Nota (favicon):** permitir `/favicon.ico` no SecurityConfig troca o 403 por **500**, porque `GlobalExceptionHandler` trata `NoResourceFoundException` como erro genérico (ver "Problemas conhecidos"). A solução adotada foi o `<link rel="icon">` data-URI — o navegador nem faz o request.

---

## ⚠️ Problemas conhecidos (pré-existentes, fora do escopo do painel)

1. **Crash-loop a cada rebuild do container.** `DataLoader` não é idempotente e o H2 é file-based dentro do volume `delivery_h2_data`; ao recriar o container, os dados duplicam/sobrescrevem e o `DataLoader` falha em loop. **Workaround:**
   ```bash
   podman compose down && podman volume rm delivery_h2_data && podman compose up -d --build --force-recreate
   ```
2. ~~**`GlobalExceptionHandler` devolve 500 para `NoResourceFoundException`**~~ — **resolvido (22/11, commit `312c438`)**: rotas e assets inexistentes retornam **404** com envelope padrão.
3. **`GET /api/relatorios/*` exige JWT** (403 sem token): o painel lê os relatórios direto do `RelatorioServiceImpl`, não pela API HTTP.

---

# 📋 Guia de Alterações (histórico — julho/2025)
**Período: Sábado Meia-noite até Segunda-feira 14/07/2025**

> ⚠️ Documento histórico mantido para referência. As versões, consultas e dados citados abaixo podem estar desatualizados — veja as seções acima para o estado atual do projeto.

## 📌 **RESUMO EXECUTIVO**
Este guia documenta todas as alterações realizadas no projeto `delivery-tech` para corrigir erros de compilação, adicionar consultas customizadas e garantir o funcionamento correto da aplicação.

---

## 🔧 **1. CORREÇÕES NO DataLoader.java**

### 📍 **Arquivo:** `src/main/java/com/deliverytech/delivery_api/config/DataLoader.java`

#### **Problemas Corrigidos:**
- ✅ Sintaxe incorreta nos métodos setter
- ✅ Chamadas para métodos inexistentes (`setTelefone()`, `setEndereco()`)
- ✅ Aspas malformadas e caracteres especiais
- ✅ Parênteses extras nos parâmetros

#### **Método `inserirClientes()` - ANTES:**
```java
Cliente clientel = new Cliente();
clientel.setNome(nome: "João Silva");
clientel.setEmail(email:"joao@email.com");
clientel.setTelefone(telefone: "11999999999");
clientel.setEndereco(endereco: "Rua A, 123");
clientel.setAtivo(ativo:true);
```

#### **Método `inserirClientes()` - DEPOIS:**
```java
Cliente cliente1 = new Cliente();
cliente1.setNome("João Silva");
cliente1.setEmail("joao@email.com");
cliente1.setAtivo(true);
```

#### **Método `inserirRestaurantes()` - Removido:**
```java
// REMOVIDO: restaurante1.setEndereco("Rua X, 123");
// MOTIVO: Método não implementado na classe Restaurante
```

---

## 🔧 **2. CORREÇÕES NOS REPOSITORIES**

### 📍 **Arquivo:** `src/main/java/com/deliverytech/delivery_api/repository/ClienteRepository.java`

#### **Problema Corrigido:**
- ✅ Método retornando `Object` em vez de `List<Cliente>`

#### **ANTES:**
```java
Object findByNomeContainingIgnoreCase(String string);
```

#### **DEPOIS:**
```java
List<Cliente> findByNomeContainingIgnoreCase(String nome);
```

---

### 📍 **Arquivo:** `src/main/java/com/deliverytech/delivery_api/repository/PedidoRepository.java`

#### **Problema Corrigido:**
- ✅ Campo `valorTotal` não existe na classe Pedido (campo correto: `total`)

#### **ANTES:**
```java
@Query("SELECT p FROM Pedido p WHERE p.valorTotal > :valor ORDER BY p.valorTotal DESC")
```

#### **DEPOIS:**
```java
@Query("SELECT p FROM Pedido p WHERE p.total > :valor ORDER BY p.total DESC")
```

---

### 📍 **Arquivo:** `src/main/java/com/deliverytech/delivery_api/repository/RestauranteRepository.java`

#### **Problema Corrigido:**
- ✅ Campo `valorTotal` não existe na classe Pedido (campo correto: `total`)

#### **ANTES:**
```java
"SUM(p.valorTotal) as totalVendas, " +
```

#### **DEPOIS:**
```java
"SUM(p.total) as totalVendas, " +
```

---

## 🆕 **3. NOVAS CONSULTAS CUSTOMIZADAS ADICIONADAS**

### 📍 **ProdutoRepository.java**
```java
@Query(value = "SELECT p.nome, COUNT(ip.produto_id) as quantidade_vendida " +
               "FROM produto p " +
               "LEFT JOIN item_pedido ip ON p.id = ip.produto_id " +
               "GROUP BY p.id, p.nome " +
               "ORDER BY quantidade_vendida DESC " +
               "LIMIT 5", nativeQuery = true)
List<Object[]> produtosMaisVendidos();
```

### 📍 **ClienteRepository.java**
```java
@Query(value = "SELECT c.nome, COUNT(p.id) as total_pedidos " +
               "FROM cliente c " +
               "LEFT JOIN pedido p ON c.id = p.cliente_id " +
               "GROUP BY c.id, c.nome " +
               "ORDER BY total_pedidos DESC " +
               "LIMIT 10", nativeQuery = true)
List<Object[]> rankingClientesPorPedidos();
```

### 📍 **RestauranteRepository.java**
```java
@Query("SELECT r.nome as nomeRestaurante, " +
       "SUM(p.total) as totalVendas, " +
       "COUNT(p.id) as quantidePedidos " +
       "FROM Restaurante r " +
       "LEFT JOIN Pedido p ON r.id = p.restaurante.id " +
       "GROUP BY r.id, r.nome")
List<RelatorioVendas> relatorioVendasPorRestaurante();
```

### 📍 **PedidoRepository.java**
```java
@Query("SELECT p.restaurante.nome, SUM(p.total) " +
       "FROM Pedido p " +
       "GROUP BY p.restaurante.nome " +
       "ORDER BY SUM(p.total) DESC")
List<Object[]> calcularTotalVendasPorRestaurante();

@Query("SELECT p FROM Pedido p WHERE p.total > :valor ORDER BY p.total DESC")
List<Pedido> buscarPedidosComValorAcimaDe(@Param("valor") BigDecimal valor);

@Query("SELECT p FROM Pedido p " +
       "WHERE p.dataPedido BETWEEN :inicio AND :fim " +
       "AND p.status = :status " +
       "ORDER BY p.dataPedido DESC")
List<Pedido> relatorioPedidosPorPeriodoEStatus(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        @Param("status") StatusPedido status);
```

---

## 🆕 **4. NOVA INTERFACE DE PROJEÇÃO CRIADA**

### 📍 **Arquivo NOVO:** `src/main/java/com/deliverytech/delivery_api/repository/RelatorioVendas.java`
```java
package com.deliverytech.delivery_api.repository;

import java.math.BigDecimal;

public interface RelatorioVendas {
    String getNomeRestaurante();
    BigDecimal getTotalVendas();
    Long getQuantidePedidos();
}
```

---

## 📦 **5. IMPORTS ADICIONADOS**

### **Nos Repositories:**
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
```

---

## ✅ **6. RESULTADO FINAL**

### **Status da Aplicação:**
- ✅ **Compilação**: Sem erros
- ✅ **Execução**: Spring Boot iniciando corretamente
- ✅ **DataLoader**: 3 clientes e 2 restaurantes inseridos
- ✅ **H2 Console**: Disponível em `http://localhost:8080/h2-console`
- ✅ **Consultas**: Todas funcionando corretamente

### **Dados de Teste Carregados:**
```
=== INICIANDO CARGA DE DADOS DE TESTE ===
--- Inserindo clientes ---
✓ 3 clientes inseridos
--- Inserindo Restaurantes ---
✓ 2 restaurantes inseridos

== TESTANDO CONSULTAS DOS REPOSITORIES ==
Cliente por email: João Silva
Clientes ativos: 2
Clientes com 'silva' no nome: 1
Existe cliente com email: true
=== CARGA DE DADOS CONCLUÍDA ===
```

---

## 🚨 **7. PRINCIPAIS LIÇÕES APRENDIDAS**

1. **Sempre verificar se os métodos existem** antes de chamá-los
2. **Conferir nomes dos campos** nas classes de modelo antes de usar em queries
3. **Sintaxe correta** nos métodos setter (sem parâmetros nomeados)
4. **Tipos de retorno** corretos nos repositories
5. **Imports necessários** para anotações e tipos

---

## 🗂️ Próximas Etapas

- [x] Adicionar campos `telefone` e `endereco` nas classes `Cliente` e `Restaurante`
- [x] Criar controllers para expor as consultas customizadas via REST API (`RelatorioController`)
- [x] Adicionar validações nos modelos (Bean Validation)
- [x] Configurar Swagger/OpenAPI para documentação da API
- [x] Implementar testes unitários e de integração (135 testes)
- [x] Cobrir com testes os repositories (queries nativas de `Produto`, `Cliente` e `Restaurante`)
- [ ] Adicionar testes de integração para restaurantes, produtos, relatórios e autenticação
- [ ] Trocar o H2 em memória por MySQL em homologação/produção
- [x] Mover `jwt.secret` para variável de ambiente — **feito (19/11, commit `bb0552b`)**: `jwt.secret=${JWT_SECRET}` sem fallback; `JwtUtil` valida a chave na subida (Base64URL ≥ 256 bits); compose exige `JWT_SECRET` via `.env`; chave antiga rotacionada e tratada como comprometida
- [x] Fechar o caminho de auth (T4) — **feito (20/11, commits `be96f51` + `ae559d1`)**: decisão = remover a interface morta. `AuthService`/`AuthServiceImpl` (stubs) removidos; `loadUserByUsername` vive no `UsuarioServiceImpl` (`UserDetailsService`, com `@Lazy` no `AuthenticationManager` para quebrar o ciclo de beans); null-guard no `JwtAuthenticationFilter` (token válido de usuário deletado → 403, não NPE/500). Fluxo real mantido: `AuthController` → `AuthenticationManager` + `UsuarioService.salvar`. +17 testes (14 `UsuarioServiceImplTest`, 3 `JwtAuthenticationFilterTest`); QG Sonar OK (new_coverage 92%)
- [x] Implementar `RelatorioServiceImpl` com dados reais (era valores fixos; agora consulta os repositories)
- [ ] Avaliar rate limiting e cache (Caffeine) nos endpoints mais acessados
- [ ] Corrigir `DataLoader` para ser idempotente (hoje exige limpar o volume `delivery_h2_data` a cada rebuild)
- [x] Corrigir `GlobalExceptionHandler` para devolver 404 em `NoResourceFoundException` — **feito (22/11, commit `312c438`)**: handler específico para `NoResourceFoundException` e `EntityNotFoundException` retornando 404 com envelope padrão (`timestamp`, `status`, `error`, `message`); null-guard em `handleRuntimeException` (sem NPE quando `message == null`) + envelope padronizado no 404; `/painel/` com barra final retorna 404; +7 testes (3 unitários em `GlobalExceptionHandlerTest`, 4 ITs em `GlobalExceptionHandlerIT`); QG Sonar OK (`new_coverage` 93,5%, 0 issues novas)
- [ ] Implementar estado de erro do painel (depende do item acima)

---

## 📁 Estrutura do Projeto
```
delivery-tech/
├── .github/workflows/ci-cd.yml      # CI/CD: testes, build do jar, imagem Docker, deploy simulado
├── .trivyignore                     # CVEs justificadas (não aplicáveis a esta API)
├── Dockerfile                       # multi-stage: Maven/JDK 21 → distroless (nonroot)
├── docker-compose.yml               # API (:8080) + Adminer (:8083)
├── docs/                            # handover do painel + diagrama (mermaid)
├── Makefile                         # atalhos: run, lint, format, test, scan, up...
├── pom.xml
├── scripts/                         # validate-tests.sh, deploy-local.sh, test-docker.sh
├── src/main/java/com/deliverytech/delivery_api/
│   ├── config/                      # SecurityConfig, DataLoader, FilterConfig,
│   │                                # ResponseCaptureFilter, OpenApiConfig, ModelMapperConfig
│   ├── controller/                  # Auth, Cliente, Restaurante, Produto, Pedido,
│   │                                # Relatorio e Health — 50 endpoints
│   ├── controller/web/              # PainelController — rotas /painel/** (Thymeleaf)
│   ├── dto/                         # request/ e response/
│   ├── exception/                   # GlobalExceptionHandler
│   ├── health/                      # Health indicators customizados
│   ├── model/                       # Cliente, Restaurante, Produto, Pedido, StatusPedido...
│   ├── projection/                  # projeções de relatório
│   ├── repository/                  # Spring Data JPA + @Query nativas
│   ├── security/                    # JwtUtil, JwtAuthenticationFilter, SecurityUtils
│   ├── service/                     # interfaces + impl/
│   ├── validation/                  # validadores (ex.: CEP)
│   └── DeliveryApiApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/                   # Thymeleaf: fragments/layout.html + painel/ (5 páginas)
│   └── static/css/painel.css        # CSS próprio do painel
└── src/test/java/                   # 4 classes *Test (unit) + 2 *IT (integração)
```

---

## 🚀 Como Executar

1. **Clone o repositório** (requisitos: **JDK 21** e Maven — o wrapper `./mvnw` é incluído)

2. **Configure o segredo JWT** (obrigatório — a aplicação não sobe sem ele):
   ```bash
   cp .env.example .env
   # gere uma chave Base64URL de >= 32 bytes e cole em JWT_SECRET no .env:
   openssl rand -base64 32 | tr '+/' '-_' | tr -d '='
   ```
   O `.env` é local e nunca versionado (`.gitignore`). Para rodar fora do compose,
   exporte a variável: `export JWT_SECRET=***

3. **Rode a aplicação:**
   ```bash
   make dev          # ou: ./mvnw spring-boot:run (com JWT_SECRET no ambiente)
   ```
   A API sobe em `http://localhost:8080` (foreground — **Ctrl+C** para parar).

4. **Acesse o H2 Console:**
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:delivery`
   - Username: `sa`
   - Password: (deixar em branco)

5. **Documentação da API:**
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - OpenAPI: `http://localhost:8080/api-docs`
   - Health: `http://localhost:8080/actuator/health`

6. **Painel web (público, sem login):**
   - Dashboard: `http://localhost:8080/painel`
   - Demais páginas: `/painel/clientes`, `/painel/produtos`, `/painel/pedidos`, `/painel/relatorios`
   - Filtros por query string (ex.: `/painel/clientes?q=ana`, `/painel/pedidos?status=CRIADO`)

> **Autenticação:** `/api/auth/**`, `/health`, `/info`, `/actuator/**`, `/painel/**` e o Swagger são públicos. Todos os demais endpoints exigem token JWT — faça login em `POST /api/auth/login` e envie o cabeçalho `Authorization: Bearer <token>`.

### 🧰 Comandos úteis (Makefile)

| Comando | Descrição |
|---|---|
| `make dev` | Sobe API + painel local (porta 8080), Ctrl+C para |
| `make lint` | Checagem de formatação (Spotless) |
| `make format` | Corrige a formatação automaticamente |
| `make test` | Testes (`mvn verify`) + cobertura JaCoCo |
| `make coverage` | Abre o relatório de cobertura |
| `make package` | Gera o `.jar` em `target/` (sem rodar testes de novo) |
| `make build` | Build da imagem Docker/Podman |
| `make scan` | Trivy: config + vulnerabilidades + segredos |
| `make up` / `make down` / `make logs` | Sobe, derruba e acompanha a stack em container (API + Adminer) |
| `make smoke` | Valida o build em container: sobe, checa endpoints e derruba |
| `make ci-local` | `lint` + `test` + `build` + `scan` antes do push |

### 🐳 Docker

Fluxo para **validar o build e o profile `docker`** — não para edição diária: cada mudança
em `src/` rebuilda o Maven dentro da imagem (minutos). Para editar, use `make dev`.

```bash
# JWT_SECRET precisa estar no .env (o compose falha sem ele — veja .env.example)
make up      # API em http://localhost:8080 e Adminer em http://localhost:8083 (retorna quando healthy)
make smoke   # sobe, checa /actuator/health, /painel e /api-docs, derruba
make down    # para os containers e apaga o volume do H2 (delivery_h2_data)
```

A imagem final usa **distroless** (sem shell, usuário `nonroot`) e roda com `-Dspring.profiles.active=docker`.

### 🧪 Testes

```bash
make test    # unitários (Surefire) + integração (Failsafe) + cobertura (JaCoCo)
```

- **104 testes unitários** em 13 classes: `ClienteServiceTest`, `PedidoServiceTest`, `ClienteControllerTest`, `GlobalExceptionHandlerTest`, `RelatorioServiceImplTest`, `UsuarioServiceImplTest`, `DataLoaderTest`, `JwtUtilTest`, `JwtAuthenticationFilterTest`, `DeliveryApiApplicationTests` + 3 `@DataJpaTest` de repositório (`ProdutoRepositoryTest`, `ClienteRepositoryTest`, `RestauranteRepositoryTest`)
- **31 testes de integração** em `ClienteControllerIT`, `PedidoControllerIT`, `PainelRelatoriosIT` e `GlobalExceptionHandlerIT` (executados pelo Failsafe no `mvn verify`)
- Relatório de cobertura: `make coverage`

---

**🎯 CONCLUSÃO:** o projeto está formatado (Spotless), testado (135 testes verdes), documentado (Swagger), com a superfície de segurança reduzida — imagem distroless, dependências corrigidas para CVEs, segredo JWT fora do código (`JWT_SECRET`) e `make scan` (Trivy) disponível para auditoria —, e agora com **painel web público** (5 páginas Thymeleaf, validado por Playwright), com o histórico de alterações preservado acima.

