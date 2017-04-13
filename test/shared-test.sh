#!/usr/bin/env bash

export SERVER=http://localhost:8080

function create-pending-upload {
    local UPLOAD_LIMIT="$1"
    TEST_DIR="/tmp/rohrli-test/`date -I`-`cat /dev/urandom | tr -cd 'a-f0-9' | head -c 16`"
    mkdir -p $TEST_DIR

    cat /dev/urandom | curl --limit-rate $UPLOAD_LIMIT -N --data-binary @- http://localhost:8080
}

function up-download {
    local SIZE_BYTES="$1"
    local UPLOAD_LIMIT="$2"
    local DOWNLOAD_LIMIT="$3"

    if [[ -z "$UPLOAD_LIMIT" ]]; then
        UPLOAD_LIMIT=1g
    fi
    if [[ -z "$DOWNLOAD_LIMIT" ]]; then
        DOWNLOAD_LIMIT=1g
    fi


    echo "Uploading $SIZE_BYTES bytes. Upload speed $UPLOAD_LIMIT, Download speed $DOWNLOAD_LIMIT"
    TEST_DIR="/tmp/rohrli-test/`date -I`-`cat /dev/urandom | tr -cd 'a-f0-9' | head -c 16`"
    mkdir -p $TEST_DIR
    TEST_FILE=$TEST_DIR/upload-file.bin
    DOWNLOAD_FILE=$TEST_DIR/download-file.bin
    cat /dev/urandom | head -c $SIZE_BYTES > $TEST_FILE

    mkfifo $TEST_DIR/notification-fifo

    cat $TEST_DIR/notification-fifo | head -n 1 | xargs curl --limit-rate $DOWNLOAD_LIMIT > $DOWNLOAD_FILE &

    cat $TEST_FILE | curl --limit-rate $UPLOAD_LIMIT -N --data-binary @- http://localhost:8080 2>/dev/null \
        | grep --line-buffered -o http://localhost:8080/.* 2>&1 > $TEST_DIR/notification-fifo

    wait

    ORIGINAL_HASH=`sha1sum $TEST_FILE | awk '{print $1}'`
    DOWNLOAD_HASH=`sha1sum $DOWNLOAD_FILE | awk '{print $1}'`

    ls -lh $TEST_DIR

    if [[ "$DOWNLOAD_HASH" != "$ORIGINAL_HASH" ]]; then
        echo "ERROR: Download file (hash $DOWNLOAD_HASH) should be equal same as original (hash: $ORIGINAL_HASH)"
        echo "TEST Dir left behind for inspection: $TEST_DIR"
        exit 1
    else
        rm -r $TEST_DIR
        echo "SUCCESS"
    fi
}