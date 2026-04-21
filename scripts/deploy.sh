#!/bin/bash
set -euo pipefail

cd /root/course-management-api

COMPOSE_FILE=docker-compose.yml
NGINX_CONF=nginx.conf
NEW_TAG=${1:-latest}

echo "Starting deployment..."

CURRENT=$(grep -o 'app-\(blue\|green\)' $NGINX_CONF | head -n1)

if [ -z "$CURRENT" ]; then
  CURRENT="app-blue"
fi

if [ "$CURRENT" = "app-blue" ]; then
  TARGET=green
  OLD=blue
else
  TARGET=blue
  OLD=green
fi

echo "Current: $CURRENT"
echo "Deploying: $TARGET"

export IMAGE_TAG_${TARGET^^}=$NEW_TAG

docker compose -f $COMPOSE_FILE --profile $TARGET up -d app-$TARGET

if ! docker ps --format '{{.Names}}' | grep -q '^course-nginx$'; then
  docker compose up -d nginx
fi

echo "Waiting for app readiness..."

SUCCESS=false

for i in {1..30}; do
  echo "Attempt $i..."
  if docker exec course-nginx curl -f http://app-$TARGET:8080/actuator/health/readiness; then
    SUCCESS=true
    break
  fi
  sleep 5
done

if [ "$SUCCESS" != "true" ]; then
  echo "Deployment failed"
  docker compose stop app-$TARGET
  docker compose rm -f app-$TARGET
  exit 1
fi

echo "Switching traffic..."

sed -i "s|app-$OLD|app-$TARGET|" $NGINX_CONF

docker exec course-nginx nginx -t
docker exec course-nginx nginx -s reload

echo "Verifying after switch..."

POST_SUCCESS=false

for i in {1..10}; do
  if curl -f http://localhost/actuator/health/readiness; then
    POST_SUCCESS=true
    break
  fi
  sleep 3
done

if [ "$POST_SUCCESS" != "true" ]; then
  echo "Post-deploy failed -> rolling back"

  sed -i "s|app-$TARGET|app-$OLD|" $NGINX_CONF

  docker exec course-nginx nginx -t
  docker exec course-nginx nginx -s reload

  docker compose stop app-$TARGET
  docker compose rm -f app-$TARGET

  echo "Rolled back to $OLD"
  exit 1
fi

echo "Cleaning up old version..."

docker compose stop app-$OLD
docker compose rm -f app-$OLD

echo "Deployment complete"