#!/bin/bash

# App launcher

echo "-- Running from config $2"

. ~/.profile
"$JAVA_HOME/bin/jps" | grep NameNode > /dev/null
RES=$?

if [ "$RES" -eq "0" ]; then
	echo "Hadoop daemons are already running and will not be automatically stopped."
else
	echo "-- Starting Hadoop daemons"
	"$HADOOP_HOME/sbin/start-dfs.sh" >& /dev/null
fi

echo "-- Running"

"$SPARK_HOME/bin/spark-submit" --master local[*] --driver-memory 4G "$1" "$2"

if [ "$RES" -ne "0" ]; then
	echo "-- Stopping Hadoop daemons"
	"$HADOOP_HOME/sbin/stop-dfs.sh" >& /dev/null
fi

echo "-- Done"