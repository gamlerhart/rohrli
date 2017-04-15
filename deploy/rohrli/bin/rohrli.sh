#!/usr/bin/env bash
INSTALL_DIR=$(cd `dirname $0` && pwd)/..

GC_LOG=$INSTALL_DIR"/logs/gc.log"
HOST=`hostname`
SYS_OUT=$INSTALL_DIR"/logs/sysout.log"
JAR_FILE=$INSTALL_DIR"/rohrli.jar"
DB_LOCATION=$INSTALL_DIR"/data/prod"
export SERVER_URL="https://rohrli.gamlor.info"

JAVA_ARGS="-Xmx96m -XX:+UseSerialGC -XX:CICompilerCount=2 -Dfile.encoding=UTF-8 -Duser.timezone=UTC \
 -Dlogback.configurationFile=$INSTALL_DIR/logback.xml -verbose:gc -Xloggc:$GC_LOG \
 -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+HeapDumpOnOutOfMemoryError \
 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1088 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.ssl=false"

cd $INSTALL_DIR

JAVA_CMD=java
if [[ -f /opt/local/java/openjdk8/bin/java ]]; then
    JAVA_CMD=/opt/local/java/openjdk8/bin/java
fi
if [[ -f $JAVA_HOME/bin/java ]]; then
    JAVA_CMD=$JAVA_HOME/bin/java
fi
$JAVA_CMD $JAVA_ARGS -jar "$JAR_FILE" "$SITE_NAME"

