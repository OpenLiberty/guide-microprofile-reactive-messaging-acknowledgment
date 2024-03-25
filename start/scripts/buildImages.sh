#!/bin/bash

echo Building images

docker pull -q icr.io/appcafe/open-liberty:kernel-slim-java11-openj9-ubi
docker pull -q bitnami/kafka:latest

docker build -t system:1.0-SNAPSHOT system/. &
docker build -t inventory:1.0-SNAPSHOT inventory/. &

wait
echo Images building completed
