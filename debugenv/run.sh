#!/bin/bash

# App launcher

echo "-- Running from config $2"

MODE=${3:-local}
case "${MODE,,}" in

  "local")
    MASTER="local[*]"
	echo "Local master."
    ;;

  "yarn")
    MASTER="yarn"
	echo "YARN master."
    ;;

  *)
    echo "Unknown run mode. Expected 'local' or 'yarn'."
	echo "-- Failed"
	exit 1
    ;;
esac

. ~/.profile
"$JAVA_HOME/bin/jps" | grep NameNode > /dev/null
RES=$?

if [[ $RES == 0 ]]; then
	echo "Hadoop daemons are already running and will not be automatically stopped."
else
	echo "-- Starting Hadoop daemons"
	"$HADOOP_HOME/sbin/start-dfs.sh" >& /dev/null
	"$HADOOP_HOME/sbin/start-yarn.sh" >& /dev/null
fi

echo "-- Running"

"$SPARK_HOME/bin/spark-submit" --master $MASTER "$1" "$2"

if [[ $RES != 0 ]]; then
	echo "-- Stopping Hadoop daemons"
	"$HADOOP_HOME/sbin/stop-dfs.sh" >& /dev/null
	"$HADOOP_HOME/sbin/stop-yarn.sh" >& /dev/null
fi

echo "-- Done"