#!/usr/bin/env bash
# usage: ./scripts/benchmark.sh [product_id] [requests]
# Run once with Redis running and once after `redis-cli FLUSHALL` before every request (NO_CACHE=1).

ID=${1:-1}
N=${2:-200}
URL="http://localhost:8080/products/$ID"

total=0
for i in $(seq 1 "$N"); do
  if [ "$NO_CACHE" = "1" ]; then
    redis-cli FLUSHALL > /dev/null
  fi
  t=$(curl -s -o /dev/null -w "%{time_total}" "$URL")
  total=$(echo "$total + $t" | bc -l)
done

echo "requests: $N"
echo "avg ms: $(echo "$total / $N * 1000" | bc -l | cut -c1-6)"
