#!/usr/bin/env bash

source ./shared-test.sh

echo curl $SERVER
curl $SERVER
if [ $? -ne 0 ]; then
    echo "FAILED"
    exit 1
fi

up-download '512'

up-download "$((2*1024*1024))" "4m" "512k"
up-download "$((2*1024*1024))" "512k" "4m"
up-download "$((128*1024*1024))" "1g" "12m"

up-download "$((1024*1024*1024))"
