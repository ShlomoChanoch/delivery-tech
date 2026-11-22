# ==============================================================================
#  Como usar o Makefile
# ==============================================================================
#
#  Uso rápido:
#    make dev        →  Sobe MySQL + API + painel local (localhost:8080) — Ctrl+C para
#    make smoke      →  Valida o build em container (MySQL + API + Adminer)
#    make test       →  Roda os testes (gera cobertura JaCoCo em target/site/)
#    make ci-local   →  Roda todas as verificações antes de todo git push
#    make help       →  Lista todos os comandos disponíveis
#
#  Requisitos: JDK 21+ e Podman/Docker (compose). O Maven NÃO precisa estar instalado:
#  usamos o wrapper (./mvnw), que baixa a versão fixada no projeto.
#
#  Engine de container: usa Podman se instalado; caso contrário, Docker.
#
#  URLs úteis da API (durante 'make dev'):
#    http://localhost:8080/painel
#    http://localhost:8080/swagger-ui/index.html
#    http://localhost:8080/api-docs
#    http://localhost:8080/actuator/health
#    MySQL local: 127.0.0.1:3306 (credenciais em .env)
#  Com 'make up'/'make smoke' há também o Adminer: http://localhost:8083
#
# ==============================================================================

# ------------------------------------------------------------------------------
#  Configuração
# ------------------------------------------------------------------------------
ENGINE       ?= $(shell command -v podman 2>/dev/null || command -v docker 2>/dev/null)
COMPOSE      ?= $(ENGINE) compose
MVN          := ./mvnw -B -ntp
IMAGE_NAME   := delivery-api
IMAGE_TAG    := dev
PORT         := 8080

.DEFAULT_GOAL := help
.PHONY: help \
        check-wrapper check-engine check-compose check-trivy \
        lint format test coverage \
        package build scan up dev down reset-db logs smoke clean \
        validate-tests deploy-local \
        ci-local

# ------------------------------------------------------------------------------
#  Ajuda
# ------------------------------------------------------------------------------
help: ## Mostra esta lista de comandos
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

check-wrapper:
	@test -f ./mvnw || { echo "Erro: ./mvnw não encontrado. Gere o projeto pelo Spring Initializr ou rode 'mvn wrapper:wrapper'."; exit 1; }
	@test -x ./mvnw || chmod +x ./mvnw
	@command -v java >/dev/null || { echo "Erro: JDK não encontrado. Instale o Java 21 ou superior."; exit 1; }

check-engine:
	@test -n "$(ENGINE)" || { echo "Erro: nenhum engine de container encontrado. Instale Podman ou Docker."; exit 1; }

check-compose: check-engine
	@$(COMPOSE) version >/dev/null 2>&1 || { echo "Erro: 'make up/down/logs/smoke' precisa de 'docker compose' (ou 'podman compose') funcionando."; exit 1; }

# ------------------------------------------------------------------------------
#  1. Execução de código
# ------------------------------------------------------------------------------
dev: check-wrapper check-compose ## Sobe MySQL (container) + API + painel local em localhost:8080 (Ctrl+C para)
	@test -f .env || { echo "Erro: arquivo .env não encontrado. Copie .env.example para .env e preencha JWT_SECRET e MYSQL_*"; exit 1; }
	@echo "🐬 Subindo MySQL 8.4..."
	$(COMPOSE) up -d --wait --wait-timeout 120 mysql
	@echo ""
	@echo "┌───────────────────────────────────────────────────────────────┐"
	@echo "│  🚀 URLs disponíveis                                         │"
	@echo "├───────────────────────────────────────────────────────────────┤"
	@printf "│  %-59s │\n" "Painel       → http://localhost:$(PORT)/painel"
	@printf "│  %-59s │\n" "Swagger UI   → http://localhost:$(PORT)/swagger-ui/index.html"
	@printf "│  %-59s │\n" "API Docs     → http://localhost:$(PORT)/api-docs"
	@printf "│  %-59s │\n" "Health       → http://localhost:$(PORT)/actuator/health"
	@printf "│  %-59s │\n" "MySQL 8.4    → 127.0.0.1:$${MYSQL_PORT:-3306}/$${MYSQL_DATABASE:-delivery}"
	@echo "└───────────────────────────────────────────────────────────────┘"
	@echo ""
	@echo "⏳ Prontas quando o Spring logar 'Started ... in Xs'."
	@echo "🛑 Para parar a API: Ctrl+C (para parar o MySQL: make down)"
	@echo ""
	bash -c 'set -a; . ./.env; set +a; \
		export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:$${MYSQL_PORT:-3306}/$${MYSQL_DATABASE:-delivery}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8"; \
		export SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"; \
		export SPRING_DATASOURCE_USERNAME="$${MYSQL_USER:-delivery}"; \
		export SPRING_DATASOURCE_PASSWORD="$${MYSQL_PASSWORD:?defina MYSQL_PASSWORD no .env}"; \
		export SPRING_H2_CONSOLE_ENABLED="false"; \
		$(MVN) spring-boot:run -Dspring-boot.run.arguments=--server.port=$(PORT)'

# ------------------------------------------------------------------------------
#  2. Qualidade de código
#     lint/format usam o plugin Spotless (google-java-format) do pom.xml
# ------------------------------------------------------------------------------
lint: check-wrapper ## Checagem de formatação (Spotless)
	$(MVN) spotless:check

format: check-wrapper ## Corrige a formatação automaticamente (Spotless)
	$(MVN) spotless:apply

test: check-wrapper ## Testes (mvn verify) + cobertura JaCoCo
	$(MVN) verify

coverage: ## Abre o relatório de cobertura gerado por 'make test'
	@test -f target/site/jacoco/index.html || { echo "Erro: rode 'make test' antes (relatório ainda não existe)."; exit 1; }
	@echo "Relatório de cobertura: target/site/jacoco/index.html"

# ------------------------------------------------------------------------------
#  3. Container
# ------------------------------------------------------------------------------
package: check-wrapper ## Gera o .jar em target/ (sem rodar testes de novo)
	$(MVN) -DskipTests package

build: package check-engine ## Build da imagem com Podman/Docker
	@test -n "$(ENGINE)" || { echo "Erro: nenhum engine de container encontrado. Instale Podman ou Docker."; exit 1; }
	$(ENGINE) build --pull -t $(IMAGE_NAME):$(IMAGE_TAG) .

check-trivy:
	@command -v trivy >/dev/null || { echo "Erro: Trivy não encontrado. Instale em https://trivy.dev"; exit 1; }

scan: check-engine check-trivy ## Trivy (config + vulnerabilidades + segredos); usa .trivyignore se existir
	trivy config --exit-code 1 --severity MEDIUM,HIGH,CRITICAL .
	trivy image --exit-code 1 --scanners vuln,secret \
		--severity HIGH,CRITICAL --ignore-unfixed $(IMAGE_NAME):$(IMAGE_TAG)

up: check-compose ## Sobe a stack (MySQL + API + Adminer) e só retorna quando pronta
	$(COMPOSE) up -d --build --wait --wait-timeout 180

down: check-compose ## Para e remove os containers (preserva o volume do MySQL)
	$(COMPOSE) down

reset-db: check-compose ## Para os containers e apaga o volume do MySQL (delivery_mysql_data)
	$(COMPOSE) down
	@$(ENGINE) volume rm delivery_mysql_data >/dev/null 2>&1 || true
	@echo "🧹 Volume delivery_mysql_data removido."

logs: check-compose ## Fica vendo os logs da API (Ctrl+C sai sem parar a API)
	$(COMPOSE) logs -f delivery-api

smoke: check-compose ## Valida o build em container: sobe, checa endpoints e derruba
	@$(MAKE) up || { $(MAKE) down; exit 1; }
	@for path in /actuator/health /painel /api-docs; do \
		code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$(PORT)$$path); \
		if [ "$$code" != "200" ]; then \
			echo "❌ $$path respondeu $$code (esperado 200)"; \
			$(MAKE) down; \
			exit 1; \
		fi; \
		echo "✅ $$path → 200"; \
	done
	$(MAKE) down

clean: check-wrapper ## Remove os artefatos de build (target/)
	$(MVN) clean

# ------------------------------------------------------------------------------
#  4. Scripts auxiliares do projeto (scripts/)
# ------------------------------------------------------------------------------
validate-tests: check-wrapper ## Roda scripts/validate-tests.sh (salva test_results.log)
	bash scripts/validate-tests.sh

deploy-local: check-engine ## Roda scripts/deploy-local.sh (deploy com Docker local)
	bash scripts/deploy-local.sh

# ------------------------------------------------------------------------------
#  5. Pipeline local
# ------------------------------------------------------------------------------
ci-local: lint test build scan ## Roda todas as verificações antes do push
