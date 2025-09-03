#!/bin/bash
if [ -f /docker-entrypoint-initdb.d/redis-init.txt ]; then
    redis-cli -h localhost -p 6379 < /docker-entrypoint-initdb.d/redis-init.txt
else
    echo "Initialization file not found: /docker-entrypoint-initdb.d/redis-init.txt"
    exit 1
fi