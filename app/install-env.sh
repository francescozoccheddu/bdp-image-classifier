#!/bin/bash

# Environment installer

USR=`whoami`
ICROOT=`realpath ~/.ic_env`
ADDIT_PROF="$ICROOT/profile"
HADOOP_HOME="$ICROOT/hadoop"
SPARK_HOME="$ICROOT/spark"
JAVA_HOME="$ICROOT/jdk"
DATA_HOME="$ICROOT/data"
NAMENODE_DATA_HOME="$DATA_HOME/namenode"
DATANODE_DATA_HOME="$DATA_HOME/datanode"
TEMP_HOME="$ICROOT/temp"

echo "-- Installing environment in $ICROOT"
mkdir -p "$ICROOT"
cd "$ICROOT"

function get {
	if [ -d "$2" ]; then
		echo "Directory already exists. Skipping."
		return
	fi
	wget "$1" -O .temp.tgz -nv --show-progress
	mkdir .temp
	tar xf .temp.tgz -C .temp
	rm -f .temp.tgz
	mv .temp/* "$2"
	rm -rf .temp
}

# Install OpenJDK 8
echo "-- Installing OpenJDK 8"
get https://builds.openlogic.com/downloadJDK/openlogic-openjdk/8u262-b10/openlogic-openjdk-8u262-b10-linux-x64.tar.gz $JAVA_HOME

# Install Hadoop 3.3.0
echo "--- Installing Hadoop 3.3.0"
get https://downloads.apache.org/hadoop/common/hadoop-3.3.0/hadoop-3.3.0-aarch64.tar.gz $HADOOP_HOME

# Install Spark 3.1.1 for Hadoop 3.2
echo "--- Installing Spark 3.1.1"
get https://downloads.apache.org/spark/spark-3.1.1/spark-3.1.1-bin-hadoop3.2.tgz $SPARK_HOME

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
export YARN_HOME=\"\$HADOOP_HOME\"
export HADOOP_COMMON_LIB_NATIVE_DIR=\"\$HADOOP_HOME/lib/native\"
export HADOOP_OPTS=-Djava.library.path=\"\$HADOOP_PREFIX/lib\"
export PATH=\"\$PATH:\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin\"
# Spark
export SPARK_HOME=\"$SPARK_HOME\"
export PATH=\"\$PATH:\$SPARK_HOME/bin:\$SPARK_HOME/sbin\"
" > "$ADDIT_PROF"
sed -i "/ic_env/d" ~/.profile 
echo ". \"$ADDIT_PROF\" # ic_env" >> ~/.profile
. ~/.profile

# Setup Hadoop
echo "-- Configuring Hadoop"
echo "
export HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true\"
export JAVA_HOME=\"$JAVA_HOME\"
export HADOOP_HOME_WARN_SUPPRESS=\"TRUE\"
export HADOOP_ROOT_LOGGER=\"WARN,DRFA\"
export HDFS_NAMENODE_USER=\"$USR\"
export HDFS_DATANODE_USER=\"$USR\"
export HDFS_SECONDARYNAMENODE_USER=\"$USR\"
export YARN_RESOURCEMANAGER_USER=\"$USR\"
export YARN_NODEMANAGER_USER=\"$USR\"
" >> "$HADOOP_CONF_DIR/hadoop-env.sh"
echo "
<configuration>
	<property>
		<name>yarn.nodemanager.aux-services</name>
		<value>mapreduce_shuffle</value>
	</property>
	<property>
		<name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name>
		<value>org.apache.hadoop.mapred.ShuffleHandler</value>
	</property>
</configuration>
" > "$HADOOP_CONF_DIR/yarn-site.xml"
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
<configuration>
	<property>
		<name>mapred.framework.name</name>
		<value>yarn</value>
	</property>
	<property>
		<name>mapreduce.jobhistory.address</name>
		<value>localhost:10020</value>
	</property>
</configuration>
" > "$HADOOP_CONF_DIR/mapred-site.xml"
mkdir -p "$DATA_HOME"
mkdir -p "$TEMP_HOME"

echo "-- Updating permissions"
chmod -R a+rwx "$ICROOT"

echo "-- Formatting HDFS"
echo "Y" | hdfs namenode -format >& /dev/null

echo "-- Done"