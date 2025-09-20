# Makefile for Docker Compose project with Maven build

# Variables
PROJECT_ROOT = $(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
COMPOSE_FILE = docker-compose.yml
PROJECT_NETWORK = backend_network

.PHONY: help clean build rebuild up start stop down logs ps restart

help:  ## Show comprehensive help message
	@echo "Docker Compose Project Management System"
	@echo "Version: 1.0"
	@echo "Usage: make [command]"
	@echo ""
	@echo "Project Configuration:"
	@echo "  Project Root:    $(PROJECT_ROOT)"
	@echo "  Docker Compose:  $(COMPOSE_FILE)"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build      - Build project with Maven and Docker images"
	@echo "  make rebuild    - Complete clean rebuild (clean + build)"
	@echo ""
	@echo "Container Management:"
	@echo "  make up         - Build and start all services (detached)"
	@echo "  make start      - Start existing containers (detached)"
	@echo "  make stop       - Stop running containers (no removal)"
	@echo "  make down       - Stop and remove containers/networks"
	@echo "  make restart    - Restart all services (down then up)"
	@echo ""
	@echo "Monitoring & Debugging:"
	@echo "  make logs       - Follow container logs (Ctrl+C to exit)"
	@echo "  make ps         - Show container status"
	@echo "  make clean      - Clean resources (keeps images)"
	@echo ""
	@echo "Notes:"
	@echo "  - All commands use: $(COMPOSE_FILE)"
	@echo "  - Add -j [N] for parallel execution (e.g., make -j 4 build)"
	@echo "  - Use V=1 for verbose output"

clean:  ## Clean build artifacts and stop containers
	@echo "Cleaning project resources..."
	@docker-compose -f $(COMPOSE_FILE) down --remove-orphans
	@echo "Clean complete: containers removed | images preserved"

build:  ## Build project with Maven and Docker
	@echo "Starting project build..."
	@cd $(PROJECT_ROOT) && mvn clean package
	@docker-compose -f $(COMPOSE_FILE) build
	@echo "Build successful:"
	@echo "  - Maven packages: $(PROJECT_ROOT)/target"
	@echo "  - Docker images:  $(COMPOSE_FILE)"

rebuild: clean build  ## Full clean rebuild

up: build run  ## Build and start services
	@echo "Services running:"
	@docker-compose -f $(COMPOSE_FILE) ps --services | xargs -I {} echo "  - {}"

start:  ## Start existing services
	@echo "Starting existing containers..."
	@docker-compose -f $(COMPOSE_FILE) start
	@echo "Services started"

run:
	@echo "Starting services in detached mode..."
	@if ! docker network inspect $(PROJECT_NETWORK) >/dev/null 2>&1; then \
		echo "Creating app network..."; \
		docker network create --label com.docker.compose.network=app_network $(PROJECT_NETWORK); \
	else \
		echo "Using existing network (soundhub_app_network)"; \
	fi
	@docker-compose -f $(COMPOSE_FILE) up -d || (echo "Failed to start services"; exit 1)

stop:  ## Stop running services
	@echo "Stopping containers..."
	@docker-compose -f $(COMPOSE_FILE) stop
	@echo "Services stopped"

down:  ## Stop and remove services
	@echo "Removing services..."
	@docker-compose -f $(COMPOSE_FILE) down --remove-orphans
	@echo "Services removed:"
	@echo "  - Containers stopped"
	@echo "  - Networks removed"
	@echo "  - Volumes preserved"

logs:  ## View service logs
	@echo "Tailing logs (Ctrl+C to exit)..."
	@docker-compose -f $(COMPOSE_FILE) logs --tail=100 -f

ps:  ## Show service status
	@echo "Container status:"
	@docker-compose -f $(COMPOSE_FILE) ps

restart: down up  ## Restart services