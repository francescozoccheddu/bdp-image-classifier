#!/bin/bash

###############################
##### Install environment #####
###############################

# Commons

if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then echo "Bash version 4 or later is required."; exit 1; fi

CANCELED=false

function log {
	echo "-- $1"
}

function die {
	echo "$1"
	uninstall
	exit 1
}

function fail {
	echo "$1"
	uninstall
	log "Failed"
	exit 1
}

function req {
	command -v $1 &> /dev/null || die "$1 is required. $2"
}

function uninstall {
	rm -rf "$ENV_DIR"
	rm -f "$INSTALL_DIR/run.sh" "$INSTALL_DIR/uninstall.sh"
}

function cancel {
	if [ "$CANCELED" = false ]; then
		CANCELED=true
		fail "Canceled"
	fi
}

trap cancel SIGHUP SIGINT SIGTERM

# Input

ARGDEF_INSTALL_DIR="env"

function help {
	echo "Setup a local Spark installation for debugging purposes. Usage:"
	echo "$0 [-h|<INSTALL_DIR>]"
	echo "Options:"
	echo "    -h            prints this help message"
	echo "    INSTALL_DIR   the installation directory (defaults to '$ARGDEF_INSTALL_DIR')"
	exit
}

while getopts ":h" OPT; do
   case $OPT in
      h) help;;
   esac
done

[ "$#" -le 1 ] || die "Expected 0 or 1 arguments but got $#. Run $0 -h for help."

ARG_INSTALL_DIR=${1:-$ARGDEF_INSTALL_DIR}

# Paths

req realpath

INSTALL_DIR=`realpath "$ARG_INSTALL_DIR"`
log "Installing environment in '$INSTALL_DIR'"
mkdir -p "$INSTALL_DIR"
THIS_DIR=`dirname "$0"`
cp "$THIS_DIR/.run.sh.template" "$INSTALL_DIR/run.sh"
cp "$THIS_DIR/.uninstall.sh.template" "$INSTALL_DIR/uninstall.sh"
cd "$INSTALL_DIR"
chmod +x "run.sh" "uninstall.sh"

# Environment variables

req whoami

ENV_DIR="$INSTALL_DIR/.image-classifier-debug-env"
HADOOP_HOME="$ENV_DIR/hadoop"
HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
HADOOP_BIN_DIR="$HADOOP_HOME/bin"
SPARK_HOME="$ENV_DIR/spark"
SPARK_CONF_DIR="$SPARK_HOME/conf"
JAVA_HOME="$ENV_DIR/jdk"
DATA_HOME="$ENV_DIR/data"
NAMENODE_DATA_HOME="$ENV_DIR/namenode"
DATANODE_DATA_HOME="$ENV_DIR/datanode"
TEMP_HOME="$ENV_DIR/temp"

USR=`whoami`
CUSTOM_ENV="
# JDK
export JAVA_HOME=\"$JAVA_HOME\"
# Hadoop
export HADOOP_HOME=\"$HADOOP_HOME\"
export HADOOP_OS_TYPE=\"`uname -s`\"
export HADOOP_CONF_DIR=\"$HADOOP_CONF_DIR\"
export HADOOP_MAPRED_HOME=\"$HADOOP_HOME\"
export HADOOP_COMMON_HOME=\"$HADOOP_HOME\"
export HADOOP_HDFS_HOME=\"$HADOOP_HOME\"
export HADOOP_COMMON_LIB_NATIVE_DIR=\"$HADOOP_HOME/lib/native\"
export HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true \"
# Spark
export SPARK_HOME=\"$SPARK_HOME\"
export SPARK_CONF_DIR=\"$SPARK_CONF_DIR\"
export HADOOP_HOME_WARN_SUPPRESS=\"TRUE\"
export HADOOP_ROOT_LOGGER=\"WARN,DRFA\"
export HDFS_NAMENODE_USER=\"$USR\"
export HDFS_DATANODE_USER=\"$USR\"
export HDFS_SECONDARYNAMENODE_USER=\"$USR\"
"

# Setup SSH

req ssh-keygen

log "Setting up SSH"

if [ -f "$ENV_DIR/ssh_tag" ]; then
	SSH_TAG=`< "$ENV_DIR/ssh_tag"`
	sed -i "/$SSH_TAG/d" ~/.ssh/authorized_keys >& /dev/null
fi
SSH_UUID=`< /proc/sys/kernel/random/uuid`
SSH_TAG="-image-classifier-debug-env-localhost-key-$SSH_UUID"
sed -i "/$SSH_TAG/d" ~/.ssh/authorized_keys >& /dev/null
if [ ! -f ~/.ssh/id_rsa ] || [ ! -f ~/.ssh/id_rsa.pub ]; then
	rm -f ~/.ssh/id_rsa ~/.ssh/id_rsa.pub
	ssh-keygen -q -t rsa -P "" -f ~/.ssh/id_rsa
fi
tr -d "\n" < ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
echo "$SSH_TAG" >> ~/.ssh/authorized_keys
echo "$SSH_TAG" > "$ENV_DIR/ssh_tag"
chmod 0600 ~/.ssh/authorized_keys

# Install packages

req curl
req tar

function get {
	if [ -d "$2" ]; then
		echo "Directory already exists. Skipping."
		return
	fi
	curl --output .temp.tgz "$1" || fail "Download failed."
	mkdir .temp
	tar xf .temp.tgz -C .temp || fail "Extraction failed."
	rm -f .temp.tgz
	mv .temp/* "$2"
	rm -rf .temp
}

mkdir -p "$ENV_DIR"

# Install OpenJDK 8

log "Installing OpenJDK 8"
ARCH=`uname -m`
case "$ARCH" in

  "x86_64")
    JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk/8u262-b10/openlogic-openjdk-8u262-b10-linux-x64.tar.gz"
    ;;

  "i386")
    JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk/8u262-b10/openlogic-openjdk-8u262-b10-linux-x32.tar.gz"
    ;;

  *)
    echo "Unknown architecture '$ARCH'. Only x86 and x64 are supported."
	log "Failed"
	exit 1
    ;;
esac
get "$JDK_URL" $JAVA_HOME


# Install Hadoop 3.2.2

log "Installing Hadoop 3.2.2"
get https://downloads.apache.org/hadoop/common/hadoop-3.2.2/hadoop-3.2.2.tar.gz $HADOOP_HOME

echo "$CUSTOM_ENV" > "$HADOOP_CONF_DIR/hadoop-env.sh"
echo "
<configuration>
	<property>
		<name>dfs.replication</name>
		<value>1</value>
	</property>
	<property>
		<name>dfs.namenode.name.dir</name>
		<value>$NAMENODE_DATA_HOME</value>
	</property>
	<property>
		<name>dfs.datanode.data.dir</name>
		<value>$DATANODE_DATA_HOME</value>
	</property>
	<property>
		<name>dfs.namenode.http-address</name>
		<value>localhost:50070</value>
	</property>
</configuration>
" > "$HADOOP_CONF_DIR/hdfs-site.xml"
echo "
<configuration>
	<property>
		<name>hadoop.tmp.dir</name>
		<value>$TEMP_HOME</value>
	</property>
	<property>
		<name>fs.default.name</name>
		<value>hdfs://localhost:9000</value>
	</property>
</configuration>
" > "$HADOOP_CONF_DIR/core-site.xml"

mkdir -p "$DATA_HOME"
mkdir -p "$TEMP_HOME"

# Install Spark 3.1.1 for Hadoop 3.2

log "Installing Spark 3.1.2"
get https://downloads.apache.org/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz $SPARK_HOME

echo "$CUSTOM_ENV" > "$SPARK_CONF_DIR/spark-env.sh"
echo "
log4j.rootCategory=ERROR, console
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.target=System.err
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{HH:mm:ss} %c{1}: %m%n
log4j.logger.image_classifier=INFO
" > "$SPARK_CONF_DIR/log4j.properties"

# Update permissions

log "Updating permissions"
chmod -R a+rwx "$ENV_DIR"

# Format HDFS

log "Formatting HDFS"
echo "Y" | "$HADOOP_BIN_DIR/hdfs" namenode -format >& /dev/null

log "Done"