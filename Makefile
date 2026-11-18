# ==============================================================================
#  Como usar o Makefile
# ==============================================================================
#
#  Uso rápido:
#    make run        →  Inicia a API Spring Boot (porta 8080)
#    make dev        →  Sobe a stack (API + Adminer) e mostra as URLs
#    make test       →  Roda os testes (gera cobertura JaCoCo em target/site/)
#    make ci-local   →  Roda todas as verificações antes de todo git push
#    make help       →  Lista todos os comandos disponíveis
#
#  Requisitos: JDK 21+ instalado. O Maven NÃO precisa estar instalado:
#  usamos o wrapper (./mvnw), que baixa a versão fixada no projeto.
#
#  Engine de container: usa Podman se instalado; caso contrário, Docker.
#
#  URLs úteis da API (após 'make dev'):
#    http://localhost:8080/swagger-ui/index.html
#    http://localhost:8080/api-docs
#    http://localhost:8080/actuator/health
#    http://localhost:8080/h2-console
#    http://localhost:8083  →  Adminer
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
        run \
        lint format test coverage \
        package build scan up dev down logs clean \
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
	@$(COMPOSE) version >/dev/null 2>&1 || { echo "Erro: 'make up/down/logs' precisa de 'docker compose' (ou 'podman compose') funcionando."; exit 1; }

# ------------------------------------------------------------------------------
#  1. Execução de código
# ------------------------------------------------------------------------------
run: check-wrapper ## Inicia a API Spring Boot (porta 8080)
	$(MVN) spring-boot:run -Dspring-boot.run.arguments=--server.port=$(PORT)

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

up: check-compose ## Sobe a stack (API + Adminer) via docker-compose.yml
	$(COMPOSE) up -d --build

dev: up ## Sobe a stack, espera a API ficar pronta e mostra as URLs
	@echo ""
	@echo "⏳ Aguardando API responder em http://localhost:$(PORT)..."
	@for i in $$(seq 1 30); do \
		code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$(PORT)/actuator/health 2>/dev/null); \
		if [ "$$code" = "200" ]; then \
			echo "✅ API pronta!"; \
			break; \
		fi; \
		[ $$i -eq 30 ] && { echo "❌ API não respondeu em 60s. Verifique: make logs"; exit 1; }; \
		sleep 2; \
	done
	@echo ""
	@echo "┌───────────────────────────────────────────────────────────────┐"
	@echo "│  🚀 URLs disponíveis                                         │"
	@echo "├───────────────────────────────────────────────────────────────┤"
	@printf "│  %-59s │\n" "Swagger UI   → http://localhost:$(PORT)/swagger-ui/index.html"
	@printf "│  %-59s │\n" "API Docs     → http://localhost:$(PORT)/api-docs"
	@printf "│  %-59s │\n" "Health       → http://localhost:$(PORT)/actuator/health"
	@printf "│  %-59s │\n" "H2 Console   → http://localhost:$(PORT)/h2-console"
	@printf "│  %-59s │\n" "Adminer      → http://localhost:8083"
	@echo "└───────────────────────────────────────────────────────────────┘"
	@echo ""
	@echo "📋 Para ver os logs:  make logs"
	@echo "🛑 Para parar:        make down"

down: check-compose ## Para e remove os containers e o volume do H2
	$(COMPOSE) down
	@# TODO: remover quando descobrirmos como resetar o H2 sem apagar o volume toda vez.
	@$(ENGINE) volume rm delivery_h2_data >/dev/null 2>&1 || true

logs: check-compose ## Fica vendo os logs da API (Ctrl+C sai sem parar a API)
	$(COMPOSE) logs -f delivery-api

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
