#!/bin/bash

# App launcher

. ~/.profile
"$JAVA_HOME/bin/jps" | grep NameNode > /dev/null
RES=$?

if [[ $RES == 0 ]]; then
	echo "-- Running from config $2"
	echo "Hadoop daemons are already running and will not be automatically stopped."
else
	echo "-- Starting Hadoop daemons"
	"$HADOOP_HOME/sbin/start-dfs.sh" >& /dev/null
	"$HADOOP_HOME/sbin/start-yarn.sh" >& /dev/null
	echo "-- Running from config $2"
fi

"$SPARK_HOME/bin/spark-submit" --master yarn "$1" "$2"

if [[ $RES != 0 ]]; then
	echo "-- Stopping Hadoop daemons"
	"$HADOOP_HOME/sbin/stop-dfs.sh" >& /dev/null
	"$HADOOP_HOME/sbin/stop-yarn.sh" >& /dev/null
fi

echo "-- Done"