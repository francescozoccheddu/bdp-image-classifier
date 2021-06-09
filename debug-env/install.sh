#!/bin/bash

# Environment installer

USR=`whoami`
IC_HOME=`realpath ~/.image-classifier-debug-env`
ADDIT_PROF="$IC_HOME/profile"
HADOOP_HOME="$IC_HOME/hadoop"
SPARK_HOME="$IC_HOME/spark"
JAVA_HOME="$IC_HOME/jdk"
DATA_HOME="$IC_HOME/data"
NAMENODE_DATA_HOME="$DATA_HOME/namenode"
DATANODE_DATA_HOME="$DATA_HOME/datanode"
TEMP_HOME="$IC_HOME/temp"

echo "-- Installing environment in $IC_HOME"
mkdir -p "$IC_HOME"
cd "$IC_HOME"

function get {
	if [ -d "$2" ]; then
		echo "Directory already exists. Skipping."
		return
	fi
	curl --output .temp.tgz "$1"
	mkdir .temp
	tar xf .temp.tgz -C .temp
	rm -f .temp.tgz
	mv .temp/* "$2"
	rm -rf .temp
}

# Setup SSH
echo "-- Setting up SSH"

if ! command -v ssh-keygen &> /dev/null
then
    echo "SSH is required. Install it and try again."
    echo "-- Failed"
    exit 1
fi

SSH_OUTPUT="key"
SSH_TAG=" # image-classifier-key"
ssh-keygen -q -t rsa -P "" -f "$SSH_OUTPUT"
touch ~/.ssh/authorized_keys
sed -i "/$SSH_TAG/d" ~/.ssh/authorized_keys
tr -d "\n" < "$SSH_OUTPUT.pub" >> ~/.ssh/authorized_keys
echo "$SSH_TAG" >> ~/.ssh/authorized_keys
chmod 0600 ~/.ssh/authorized_keys
rm -f "$SSH_OUTPUT" "$SSH_OUTPUT.pub"

# Install OpenJDK 8
echo "-- Installing OpenJDK 8"
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
	echo "-- Failed"
	exit 1
    ;;
esac
get "$JDK_URL" $JAVA_HOME

# Install Hadoop 3.2.2
echo "--- Installing Hadoop 3.2.2"
get https://downloads.apache.org/hadoop/common/hadoop-3.2.2/hadoop-3.2.2.tar.gz $HADOOP_HOME

# Install Spark 3.1.1 for Hadoop 3.2
echo "--- Installing Spark 3.1.2"
get https://downloads.apache.org/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz $SPARK_HOME

# Update profile
echo "-- Extending ~/.profile with $ADDIT_PROF"
echo "
# JDK
export JAVA_HOME=\"$JAVA_HOME\"
export PATH=\"\$PATH:\$JAVA_HOME/bin\"
# Hadoop
export HADOOP_HOME=\"$HADOOP_HOME\"
export HADOOP_CONF_DIR=\"\$HADOOP_HOME/etc/hadoop\"
export HADOOP_MAPRED_HOME=\"\$HADOOP_HOME\"
export HADOOP_COMMON_HOME=\"\$HADOOP_HOME\"
export HADOOP_HDFS_HOME=\"\$HADOOP_HOME\"
export HADOOP_COMMON_LIB_NATIVE_DIR=\"\$HADOOP_HOME/lib/native\"
export LD_LIBRARY_PATH=\"\$HADOOP_COMMON_LIB_NATIVE_DIR\"
export HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true -Djava.library.path=\\\"\$HADOOP_COMMON_LIB_NATIVE_DIR\\\"\"
export PATH=\"\$PATH:\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin\"
# Spark
export SPARK_HOME=\"$SPARK_HOME\"
export PATH=\"\$PATH:\$SPARK_HOME/bin:\$SPARK_HOME/sbin\"
" > "$ADDIT_PROF"
sed -i "/# image-classifier-debug-env profile/d" ~/.profile 
echo ". \"$ADDIT_PROF\" # image-classifier-debug-env profile" >> ~/.profile
. ~/.profile

# Setup Hadoop
echo "-- Configuring Hadoop"
echo "
export HADOOP_OS_TYPE=\"\${HADOOP_OS_TYPE:-\$(uname -s)}\"
export JAVA_HOME=\"$JAVA_HOME\"
export HADOOP_HOME_WARN_SUPPRESS=\"TRUE\"
export HADOOP_ROOT_LOGGER=\"WARN,DRFA\"
export HDFS_NAMENODE_USER=\"$USR\"
export HDFS_DATANODE_USER=\"$USR\"
export HDFS_SECONDARYNAMENODE_USER=\"$USR\"
" > "$HADOOP_CONF_DIR/hadoop-env.sh"
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
echo "
log4j.rootCategory=ERROR, console
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.target=System.err
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{HH:mm:ss} %c{1}: %m%n
log4j.logger.image_classifier=INFO
" > "$SPARK_HOME/conf/log4j.properties"
mkdir -p "$DATA_HOME"
mkdir -p "$TEMP_HOME"

echo "-- Updating permissions"
chmod -R a+rwx "$IC_HOME"

echo "-- Formatting HDFS"
echo "Y" | hdfs namenode -format >& /dev/null

echo "-- Done"