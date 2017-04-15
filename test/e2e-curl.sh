#!/usr/bin/env bash

TESTDIR=`dirname $0`
APPDIR=`readlink -f $TESTDIR/../`
export SERVER=http://localhost
SERVER_PID_FILE=$APPDIR/target/e2e-server.pid

if [ -e $SERVER_PID_FILE ]; then
    echo "Killing old server, if sill running `cat $SERVER_PID_FILE`"
    OLD_PID=`cat $SERVER_PID_FILE`
    ps --ppid $OLD_PID -o pid= | xargs kill
    kill $OLD_PID
    rm $SERVER_PID_FILE
fi

cd $APPDIR
echo "starting of directory $APPDIR. Running in `pwd`"
echo "starting server $SERVER. PID file $SERVER_PID_FILE"
lein run > $APPDIR/target/test-server.log &
SERVER_PID=$!
echo $SERVER_PID > $SERVER_PID_FILE
echo "started server $SERVER at $SERVER_PID"

curl $SERVER
while [ $? -ne 0 ]; do
    echo "server no up, waiting for $SERVER"
    sleep 1
    curl "$SERVER"
done


echo "Stopping in 5 seconds"
sleep 5

ps --ppid $SERVER_PID -o pid= | xargs kill
kill $SERVER_PID
rm $SERVER_PID_FILE


