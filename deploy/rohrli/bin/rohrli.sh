#!/usr/bin/env bash
INSTALL_DIR=$(cd `dirname $0` && pwd)/..

GC_LOG=$INSTALL_DIR"/logs/gc.log"
HOST=`hostname`
SYS_OUT=$INSTALL_DIR"/logs/sysout.log"
JAR_FILE=$INSTALL_DIR"/rohrli.jar"
DB_LOCATION=$INSTALL_DIR"/data/prod"

JAVA_ARGS="-Xmx512m -XX:+UseSerialGC -XX:CICompilerCount=2 -Dfile.encoding=UTF-8 -Duser.timezone=UTC \
 -Dlogback.configurationFile=$INSTALL_DIR/logback.xml -verbose:gc -Xlog:gc,gc+cpu=info::utc:$GC_LOG \
 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$INSTALL_DIR/logs \
 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1088 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.ssl=false"

cd $INSTALL_DIR

JAVA_CMD=java
$JAVA_CMD $JAVA_ARGS -jar "$JAR_FILE"

