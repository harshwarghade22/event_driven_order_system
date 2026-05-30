# Reset order.created topic (requires delete.topic.enable=true on Kafka broker)
#
# Prerequisites:
#   1. Stop inventory_service (Ctrl+C) — active consumer blocks reliable topic delete
#   2. Use Kafka from docker-compose.yml (has KAFKA_DELETE_TOPIC_ENABLE=true)
#
# First-time or if delete fails today:
#   docker stop kafka
#   docker rm kafka
#   cd d:\event_driver_order_system
#   docker compose up -d
#
# Then run this script:
#   .\scripts\reset-order-created-topic.ps1

$ErrorActionPreference = "Stop"

Write-Host "Checking Kafka container..."
docker ps --filter "name=kafka" --format "{{.Names}}" | Out-Null
if ($LASTEXITCODE -ne 0 -or -not (docker ps -q --filter "name=kafka")) {
    Write-Error "Kafka container 'kafka' is not running. Start with: docker compose up -d"
}

Write-Host "Listing topics..."
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

Write-Host ""
Write-Host "Deleting topic order.created (ignored by broker if delete.topic.enable=false)..."
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
  --bootstrap-server localhost:9092 `
  --delete `
  --topic order.created 2>&1

Start-Sleep -Seconds 5

$topics = docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>&1
if ($topics -match "order\.created") {
    Write-Host ""
    Write-Host "Topic still exists — delete.topic.enable is likely false on this broker."
    Write-Host "Recreate Kafka with deletion enabled:"
    Write-Host "  docker stop kafka && docker rm kafka"
    Write-Host "  cd d:\event_driver_order_system"
    Write-Host "  docker compose up -d"
    Write-Host "  Then run this script again."
    exit 1
}

Write-Host "Creating topic order.created..."
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
  --bootstrap-server localhost:9092 `
  --create `
  --topic order.created `
  --partitions 1 `
  --replication-factor 1

Write-Host ""
Write-Host "Topic ready:"
docker exec kafka /opt/kafka/bin/kafka-topics.sh `
  --bootstrap-server localhost:9092 `
  --describe `
  --topic order.created

Write-Host ""
Write-Host "Optional — clean idempotency state in MySQL:"
Write-Host "  TRUNCATE TABLE inventory_db.processed_events;"
Write-Host ""
Write-Host "Start order_service + inventory_service, then POST /orders for fresh events."
