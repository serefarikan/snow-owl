#!/bin/bash

SCRIPT="$0"

# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

# determine kernel home
KERNEL_HOME=`dirname "$SCRIPT"`/..

# make KERNEL_HOME absolute
KERNEL_HOME=`cd "$KERNEL_HOME"; pwd`

shopt -s extglob

# start the kernel
if [[ "$CONFIG_DIR" != /* ]]
then
    CONFIG_DIR=$KERNEL_HOME/$CONFIG_DIR
fi

CONFIG_AREA=$KERNEL_HOME/work

if [ ! -z $cygwin ]; then
    KERNEL_HOME=$(cygpath -wp $KERNEL_HOME)
    CONFIG_AREA=$(cygpath -wp $CONFIG_AREA)
fi

if [ -z "$JAVA_HOME" ]
then
        JAVA_EXECUTABLE=$KERNEL_HOME/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_17.*/jre/bin/java
else
        JAVA_EXECUTABLE=$JAVA_HOME/bin/java
fi

TMP_DIR=$KERNEL_HOME/work/tmp
# Ensure that the tmp directory exists
mkdir -p "$TMP_DIR"

SO_JAVA_OPTS="-Xms6g \
                -Xmx6g \
                -XX:+AlwaysPreTouch \
                -Xss1m \
                -server \
                -Djava.awt.headless=true \
                -Dosgi.noShutdown=true \
                -Dosgi.classloader.type=nonparallel \
                -Dosgi.console=2501 \
                -Djetty.http.port=8080 \
                -XX:+HeapDumpOnOutOfMemoryError \
                --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
                --add-opens java.base/java.lang=ALL-UNNAMED \
                --add-opens java.base/java.util=ALL-UNNAMED \
                --add-opens java.base/java.time=ALL-UNNAMED \
                --add-opens java.base/sun.security.x509=ALL-UNNAMED \
                --add-opens java.base/java.text=ALL-UNNAMED \
                --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
                -Djdk.security.defaultKeySize=DSA:1024 \
                $SO_JAVA_OPTS"

pushd "$KERNEL_HOME"

exec $JAVA_EXECUTABLE $SO_JAVA_OPTS \
  -Djava.io.tmpdir="$TMP_DIR" \
  -Dosgi.install.area="$KERNEL_HOME" \
  -Dosgi.configuration.area="$CONFIG_AREA" \
  -jar plugins/org.eclipse.equinox.launcher_1.6.300.v20210813-1054.jar

popd
