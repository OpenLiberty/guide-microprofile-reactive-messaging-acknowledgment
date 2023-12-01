#!/bin/bash

NETWORK=reactive-app

docker network create $NETWORK

docker run -d \
    -e ALLOW_PLAINTEXT_LISTENER=yes \
    -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
    -e KAFKA_CFG_NODE_ID=0 \
    -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
    -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
    -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093 \
    -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -p 9092:9092 \
    --hostname=kafka \
    --network=$NETWORK \
    --name=kafka \
    --rm \
    bitnami/kafka:latest &

wait