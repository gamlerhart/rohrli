#!/usr/bin/env bash

source ./shared-test.sh

up-download "$((20*1024*1024))" "8m" "512k"
