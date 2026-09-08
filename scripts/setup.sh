#!/bin/bash
set -e

echo "=== [1/3] Khoi dong cum 4 Backend Servers bang Docker Compose ==="
docker compose up -d

echo "=== [2/3] Chay toan bo Unit Tests ==="
mvn test

echo "=== [3/3] Build goi ung dung Load Balancer ==="
mvn clean package -DskipTests

echo "Setup hoan tat! He thong da san sang."