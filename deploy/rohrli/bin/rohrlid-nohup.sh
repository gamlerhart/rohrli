#!/usr/bin/env bash
# My system.d unit file from hell...duck and cover ;)
SCRIPT_DIR=$(cd `dirname $0` && pwd)
INSTALL_DIR="$SCRIPT_DIR/.."

echo "Install dir: $INSTALL_DIR, user `whoami`"
echo "Killing old instance"
pkill --signal SIGTERM -f $SCRIPT_DIR/rohrli.sh
wait 3
pkill --signal SIGKILL -f $SCRIPT_DIR/rohrli.sh
wait 1
echo "Killing starting new instance"
nohup $SCRIPT_DIR/rohrli.sh 2>1 > $INSTALL_DIR/logs/std-err-out.txt &

nohup sudo -u rohrlid -g rohrlid $SCRIPT_DIR/rohrli.sh 2>&1 > $INSTALL_DIR/logs/std-err-out.txt