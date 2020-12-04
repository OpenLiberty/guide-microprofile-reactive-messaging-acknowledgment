#!/bin/bash

echo Building images

docker pull openliberty/open-liberty:kernel-java8-openj9-ubi
start /b docker pull bitnami/zookeeper:3
start /b docker pull bitnami/kafka:2

docker build -t system:1.0-SNAPSHOT system/. &
docker build -t inventory:1.0-SNAPSHOT inventory/. &

wait
echo Images building completed
