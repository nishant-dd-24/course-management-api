#!/bin/bash
set -euo pipefail

# ──────────────────────────────────────────────────────────────────────────────
# Initialization & Setup
# ──────────────────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Starting bootstrap process...${NC}"

# Make script executable from any directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." >/dev/null 2>&1 && pwd)"
cd "$PROJECT_ROOT" || {
  echo -e "${RED}Error: Failed to navigate to project root directory '$PROJECT_ROOT'.${NC}"
  echo -e "${YELLOW}Check script location and permissions.${NC}"
  exit 1
}
echo -e "${GREEN}Running in project root: $PROJECT_ROOT${NC}"

# ──────────────────────────────────────────────────────────────────────────────
# Helper Functions
# ──────────────────────────────────────────────────────────────────────────────

# Safely load .env without evaluating subshells or executing commands
load_env() {
    local env_file="$1"
    [[ ! -f "$env_file" ]] && return 0
    
    while IFS= read -r line || [[ -n "$line" ]]; do
        # Ignore comments and empty lines
        [[ "$line" =~ ^[[:space:]]*# ]] || [[ "$line" =~ ^[[:space:]]*$ ]] && continue
        
        # Extract key and value
        local key="${line%%=*}"
        local value="${line#*=}"
        
        # Trim whitespace robustly (POSIX compatible way without extglob)
        key="${key#"${key%%[![:space:]]*}"}"
        key="${key%"${key##*[![:space:]]}"}"
        value="${value#"${value%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        
        # Remove surrounding quotes safely
        if [[ "$value" == '"'*'"' ]] || [[ "$value" == \'*\' ]]; then
            value="${value:1:-1}"
        fi
        
        export "$key"="$value"
    done < "$env_file"
}

# Wait for a docker compose service to reach desired state
wait_for_container() {
  local service="$1"
  local expect_health="$2" # "true" for healthy, "false" for just running
  local max_attempts="${3:-30}"
  local sleep_time="${4:-2}"
  local attempt=1
  
  echo -e "${YELLOW}Waiting for service '$service' to be ready...${NC}"
  
  local container_id
  container_id=$(docker compose ps -q "$service" 2>/dev/null || true)
  
  if [[ -z "$container_id" ]]; then
    echo -e "${RED}Error: Container for service '$service' not found.${NC}"
    echo -e "${YELLOW}Check if the service failed to start: docker compose logs $service${NC}"
    exit 1
  fi
  
  while (( attempt <= max_attempts )); do
    container_id=$(docker compose ps -q "$service" 2>/dev/null || true)
    local status
    if [[ "$expect_health" == "true" ]]; then
      status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}unknown{{end}}' "$container_id" 2>/dev/null || true)
      if [[ "$status" == "healthy" ]]; then
        echo -e "${GREEN}Service '$service' is healthy.${NC}"
        return 0
      fi
    else
      status=$(docker inspect -f '{{.State.Status}}' "$container_id" 2>/dev/null || true)
      if [[ "$status" == "running" ]]; then
        echo -e "${GREEN}Service '$service' is running.${NC}"
        return 0
      fi
    fi
    sleep "$sleep_time"
    ((attempt++))
  done
  
  echo -e "${RED}Error: Timed out waiting for '$service' after $((max_attempts * sleep_time)) seconds.${NC}"
  echo -e "${YELLOW}Please inspect the logs: docker compose logs $service${NC}"
  exit 1
}

# ──────────────────────────────────────────────────────────────────────────────
# 1-3. Prerequisites & Validation
# ──────────────────────────────────────────────────────────────────────────────

echo -e "${YELLOW}Verifying prerequisites...${NC}"
for cmd in docker git curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo -e "${RED}Error: '$cmd' is not installed.${NC}"
    echo -e "${YELLOW}Please install $cmd before running this script.${NC}"
    exit 1
  fi
done

if ! docker compose version >/dev/null 2>&1; then
  echo -e "${RED}Error: 'docker compose' is not available.${NC}"
  echo -e "${YELLOW}Ensure Docker Compose V2 is installed and accessible.${NC}"
  exit 1
fi

echo -e "${YELLOW}Verifying Docker daemon...${NC}"
if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}Error: Docker daemon is not running or not accessible.${NC}"
  echo -e "${YELLOW}Start the Docker daemon and check permissions (e.g. sudo).${NC}"
  exit 1
fi

echo -e "${YELLOW}Verifying required project files...${NC}"
for file in .env docker-compose.yml nginx.conf; do
  if [[ ! -f "$file" ]]; then
    echo -e "${RED}Error: Required file '$file' not found in $PROJECT_ROOT.${NC}"
    echo -e "${YELLOW}Ensure you have correctly set up the project files.${NC}"
    exit 1
  fi
done
echo -e "${GREEN}All prerequisites and files verified.${NC}"

# Load environment safely
load_env .env

# Configure domains from environment or use defaults
DOMAIN_API="${DOMAIN_API:-api.nishantdd.dev}"
DOMAIN_APP="${DOMAIN_APP:-app.nishantdd.dev}"
ADMIN_EMAIL="${APP_ADMIN_EMAIL:-admin@nishantdd.dev}"

# ──────────────────────────────────────────────────────────────────────────────
# 4. Certificates Bootstrapping
# ──────────────────────────────────────────────────────────────────────────────

echo -e "${YELLOW}Checking Let's Encrypt certificates...${NC}"
# Use a direct exit code check without a subshell pipe
if docker compose run --rm --entrypoint sh certbot -c "[ -d /etc/letsencrypt/live/${DOMAIN_API} ]" >/dev/null 2>&1; then
  HAS_CERTS=1
  echo -e "${GREEN}Certificates already exist.${NC}"
else
  HAS_CERTS=0
  echo -e "${YELLOW}Certificates not found. Generating dummy certificates for Nginx startup...${NC}"
  docker compose run --rm --entrypoint sh certbot -c "
    apk add --no-cache openssl && \
    mkdir -p /etc/letsencrypt/live/${DOMAIN_API} /etc/letsencrypt/live/${DOMAIN_APP} && \
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 -keyout /etc/letsencrypt/live/${DOMAIN_API}/privkey.pem -out /etc/letsencrypt/live/${DOMAIN_API}/fullchain.pem -subj '/CN=${DOMAIN_API}' && \
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 -keyout /etc/letsencrypt/live/${DOMAIN_APP}/privkey.pem -out /etc/letsencrypt/live/${DOMAIN_APP}/fullchain.pem -subj '/CN=${DOMAIN_APP}'
  "
  echo -e "${GREEN}Dummy certificates generated.${NC}"
fi

# ──────────────────────────────────────────────────────────────────────────────
# 5-6. Infrastructure Startup & Verification
# ──────────────────────────────────────────────────────────────────────────────

echo -e "${YELLOW}Starting infrastructure containers (postgres, redis, nginx)...${NC}"
docker compose up -d postgres redis nginx

wait_for_container "postgres" "true" 30 2
wait_for_container "redis" "true" 30 2
wait_for_container "nginx" "false" 15 2

echo -e "${GREEN}Infrastructure is up and healthy.${NC}"

# ──────────────────────────────────────────────────────────────────────────────
# 7. Real Certificate Acquisition
# ──────────────────────────────────────────────────────────────────────────────

if [[ "$HAS_CERTS" == "0" ]]; then
  echo -e "${YELLOW}Requesting Let's Encrypt certificates...${NC}"
  # Remove dummy certificates cleanly using direct arguments instead of 'sh -c'
  docker compose run --rm --entrypoint rm certbot -rf "/etc/letsencrypt/live/${DOMAIN_API}" "/etc/letsencrypt/live/${DOMAIN_APP}" "/etc/letsencrypt/archive/${DOMAIN_API}" "/etc/letsencrypt/archive/${DOMAIN_APP}"
  
  # Obtain real certificates
  docker compose run --rm certbot certonly --webroot -w /var/www/certbot \
    --email "${ADMIN_EMAIL}" \
    -d "${DOMAIN_API}" -d "${DOMAIN_APP}" \
    --rsa-key-size 4096 \
    --agree-tos \
    --force-renewal \
    --non-interactive

  echo -e "${YELLOW}Reloading Nginx to apply new certificates...${NC}"
  NGINX_CONTAINER=$(docker compose ps -q nginx 2>/dev/null || true)
  if [[ -n "$NGINX_CONTAINER" ]]; then
    docker exec "$NGINX_CONTAINER" nginx -s reload
    echo -e "${GREEN}Certificates acquired and Nginx reloaded.${NC}"
  else
    echo -e "${RED}Error: Nginx container not found during reload phase.${NC}"
    exit 1
  fi
fi

# ──────────────────────────────────────────────────────────────────────────────
# 8-9. Application Startup & Verification
# ──────────────────────────────────────────────────────────────────────────────

echo -e "${YELLOW}Determining initial application target from nginx.conf...${NC}"
# Extract the active app from config, default to app-blue if no match is found
INITIAL_APP=$(grep -m1 -o 'app-\(blue\|green\)' nginx.conf || true)
INITIAL_APP="${INITIAL_APP:-app-blue}"

echo -e "${GREEN}Selected '$INITIAL_APP' as the initial application.${NC}"
echo -e "${YELLOW}Starting $INITIAL_APP...${NC}"

if [[ "$INITIAL_APP" == "app-green" ]]; then
  docker compose --profile green up -d app-green
else
  docker compose up -d app-blue
fi

wait_for_container "$INITIAL_APP" "true" 40 5

# ──────────────────────────────────────────────────────────────────────────────
# 10. End-to-End Verification
# ──────────────────────────────────────────────────────────────────────────────

echo -e "${YELLOW}Verifying backend and frontend accessibility...${NC}"

echo -e "${YELLOW}Checking backend (https://${DOMAIN_API}/actuator/health/readiness)...${NC}"
if ! curl -k -s -f "https://${DOMAIN_API}/actuator/health/readiness" > /dev/null; then
  echo -e "${RED}Error: Backend verification failed at https://${DOMAIN_API}/actuator/health/readiness.${NC}"
  echo -e "${YELLOW}Check the application logs (docker compose logs $INITIAL_APP) and nginx logs (docker compose logs nginx).${NC}"
  exit 1
fi

echo -e "${YELLOW}Checking frontend (https://${DOMAIN_APP})...${NC}"
if ! curl -k -s -f "https://${DOMAIN_APP}" > /dev/null; then
  echo -e "${RED}Error: Frontend verification failed at https://${DOMAIN_APP}.${NC}"
  echo -e "${YELLOW}Check the frontend volume mount or nginx logs.${NC}"
  exit 1
fi

echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}Bootstrap completed successfully!${NC}"
echo -e "${GREEN}==========================================${NC}"
