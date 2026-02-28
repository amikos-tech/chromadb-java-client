# ChromaDB Java Client Makefile
# This Makefile provides convenient commands for common development tasks

# Variables
MAVEN := mvn
JAVA := java
CHROMA_VERSIONS :=

# Color output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[0;33m
BLUE := \033[0;34m
NC := \033[0m # No Color

# Default target
.DEFAULT_GOAL := help

# Check for required tools
.PHONY: check-tools
check-tools:
	@command -v $(JAVA) >/dev/null 2>&1 || { echo "$(RED)Error: Java is not installed$(NC)"; exit 1; }
	@command -v $(MAVEN) >/dev/null 2>&1 || { echo "$(RED)Error: Maven is not installed$(NC)"; exit 1; }
	@echo "$(GREEN)✓ All required tools are installed$(NC)"

##@ Core Build Targets

.PHONY: build
build: check-tools ## Clean and compile the project
	@echo "$(BLUE)Building project...$(NC)"
	$(MAVEN) clean compile

.PHONY: test
test: check-tools ## Run all tests
	@echo "$(BLUE)Running tests...$(NC)"
	$(MAVEN) test

.PHONY: package
package: check-tools ## Create JAR package
	@echo "$(BLUE)Creating JAR package...$(NC)"
	$(MAVEN) clean package

.PHONY: install
install: check-tools ## Install to local Maven repository
	@echo "$(BLUE)Installing to local Maven repository...$(NC)"
	$(MAVEN) clean install

.PHONY: clean
clean: check-tools ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(MAVEN) clean

##@ Testing Targets

.PHONY: test-unit
test-unit: check-tools ## Run unit tests only
	@echo "$(BLUE)Running unit tests...$(NC)"
	$(MAVEN) test -Dtest="!TestAPI,!*Integration*"

.PHONY: test-integration
test-integration: check-tools ## Run integration tests only
	@echo "$(BLUE)Running integration tests...$(NC)"
	$(MAVEN) test -Dtest="TestAPI,*Integration*"

.PHONY: test-version
test-version: check-tools ## Test with specific ChromaDB version (use CHROMA_VERSION=x.x.x)
ifndef CHROMA_VERSION
	@echo "$(RED)Error: CHROMA_VERSION not specified$(NC)"
	@echo "Usage: make test-version CHROMA_VERSION=1.0.0"
	@exit 1
endif
	@echo "$(BLUE)Testing with ChromaDB version $(CHROMA_VERSION)...$(NC)"
	CHROMA_VERSION=$(CHROMA_VERSION) $(MAVEN) test

.PHONY: test-class
test-class: check-tools ## Run specific test class (use TEST=ClassName)
ifndef TEST
	@echo "$(RED)Error: TEST not specified$(NC)"
	@echo "Usage: make test-class TEST=TestAPI"
	@exit 1
endif
	@echo "$(BLUE)Running test class $(TEST)...$(NC)"
	$(MAVEN) test -Dtest=$(TEST)

.PHONY: test-method
test-method: check-tools ## Run specific test method (use TEST=ClassName#methodName)
ifndef TEST
	@echo "$(RED)Error: TEST not specified$(NC)"
	@echo "Usage: make test-method TEST=TestAPI#testCreateCollection"
	@exit 1
endif
	@echo "$(BLUE)Running test method $(TEST)...$(NC)"
	$(MAVEN) test -Dtest=$(TEST)

##@ Code Generation

.PHONY: generate
generate: check-tools ## Generate API client from OpenAPI spec
	@echo "$(BLUE)Generating API client from OpenAPI spec...$(NC)"
	$(MAVEN) generate-sources

.PHONY: generate-clean
generate-clean: check-tools ## Clean and regenerate API client
	@echo "$(BLUE)Cleaning generated sources...$(NC)"
	rm -rf target/generated-sources
	@echo "$(BLUE)Regenerating API client...$(NC)"
	$(MAVEN) generate-sources

##@ Development Utilities

.PHONY: deps
deps: check-tools ## Download/update dependencies
	@echo "$(BLUE)Resolving dependencies...$(NC)"
	$(MAVEN) dependency:resolve

.PHONY: deps-tree
deps-tree: check-tools ## Display dependency tree
	@echo "$(BLUE)Displaying dependency tree...$(NC)"
	$(MAVEN) dependency:tree

.PHONY: deps-analyze
deps-analyze: check-tools ## Analyze dependencies for conflicts
	@echo "$(BLUE)Analyzing dependencies...$(NC)"
	$(MAVEN) dependency:analyze

.PHONY: versions
versions: check-tools ## Check for dependency updates
	@echo "$(BLUE)Checking for dependency updates...$(NC)"
	$(MAVEN) versions:display-dependency-updates

.PHONY: compile
compile: check-tools ## Compile source code without cleaning
	@echo "$(BLUE)Compiling source code...$(NC)"
	$(MAVEN) compile

.PHONY: test-compile
test-compile: check-tools ## Compile test source code
	@echo "$(BLUE)Compiling test source code...$(NC)"
	$(MAVEN) test-compile

##@ Release Targets

.PHONY: release-prepare
release-prepare: check-tools ## Prepare for release (update versions)
	@echo "$(YELLOW)Preparing for release...$(NC)"
	@echo "Current version: $$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout)"
	@read -p "Enter new version: " VERSION && \
	$(MAVEN) versions:set -DnewVersion=$$VERSION && \
	echo "$(GREEN)✓ Version updated to $$VERSION$(NC)"

.PHONY: release-rollback
release-rollback: check-tools ## Rollback version changes
	@echo "$(YELLOW)Rolling back version changes...$(NC)"
	$(MAVEN) versions:revert

.PHONY: snapshot
snapshot: check-tools ## Deploy snapshot version
	@echo "$(BLUE)Deploying snapshot...$(NC)"
	$(MAVEN) clean deploy

.PHONY: release
release: check-tools ## Full release (CI only - requires GPG signing)
	@echo "$(YELLOW)⚠️  This target is intended for CI use only$(NC)"
	@echo "$(BLUE)Performing full release...$(NC)"
	$(MAVEN) clean deploy -P release

##@ Docker Targets

.PHONY: docker-test
docker-test: ## Run tests in Docker environment
	@echo "$(BLUE)Running tests in Docker...$(NC)"
	docker run --rm -v $(PWD):/workspace -w /workspace maven:3-openjdk-8 mvn test

.PHONY: docker-build
docker-build: ## Build project in Docker environment
	@echo "$(BLUE)Building in Docker...$(NC)"
	docker run --rm -v $(PWD):/workspace -w /workspace maven:3-openjdk-8 mvn clean package

##@ Utility Targets

.PHONY: info
info: check-tools ## Display project information
	@echo "$(BLUE)Project Information:$(NC)"
	@echo "  Name: $$($(MAVEN) help:evaluate -Dexpression=project.name -q -DforceStdout)"
	@echo "  Group ID: $$($(MAVEN) help:evaluate -Dexpression=project.groupId -q -DforceStdout)"
	@echo "  Artifact ID: $$($(MAVEN) help:evaluate -Dexpression=project.artifactId -q -DforceStdout)"
	@echo "  Version: $$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout)"
	@echo "  Java Version: $$($(JAVA) -version 2>&1 | head -1)"
	@echo "  Maven Version: $$($(MAVEN) -version | head -1)"

.PHONY: help
help: ## Display this help message
	@echo "ChromaDB Java Client - Development Tasks"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "$(YELLOW)Available targets:$(NC)\n"} \
		/^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 } \
		/^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)Examples:$(NC)"
	@echo "  make build                     # Build the project"
	@echo "  make test                      # Run all tests"
	@echo "  make test-version CHROMA_VERSION=1.0.0   # Test with specific version"
	@echo "  make test-class TEST=TestAPI   # Run specific test class"
	@echo "  make help                      # Show this help"

# Quick shortcuts
.PHONY: b
b: build ## Shortcut for build

.PHONY: t
t: test ## Shortcut for test

.PHONY: c
c: clean ## Shortcut for clean

.PHONY: i
i: install ## Shortcut for install