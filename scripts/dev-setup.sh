#!/usr/bin/env bash
# Bash script to quickly bootstrap local development environment
set -e

echo "========================================="
echo " Starting IAM Server Local Dev Services  "
echo "========================================="

# Start PostgreSQL and Redis via Docker Compose
docker compose up -d postgres redis

echo "Waiting for services to become healthy..."
sleep 3

docker compose ps

echo ""
echo "Environment ready! You can now run: gradle bootRun"
