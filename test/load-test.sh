#!/usr/bin/env bash

PIDS=""
for i in {1..30}; do
    ( ./worker-slow-upload.sh $i 2>&1 > /dev/null ) &
    PIDS+=" $!"
done
for i in {1..30}; do
    ( ./worker-slow-download.sh $i 2>&1 > /dev/null ) &
    PIDS+=" $!"
done

for p in $PIDS; do
    if wait $p; then
        echo "Process $p success"
    else
        echo "Process $p failed"
        echo "FAILURE: Small load test"
        exit 1
    fi
done


echo "Full success"
