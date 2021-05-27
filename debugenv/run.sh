#!/bin/bash

# App launcher

jps | grep NameNode > /dev/null
RES=$?


if [[ $RES == 0 ]]; then
	echo "-- Running from config $2"
	echo "Hadoop daemons are already running and will not be automatically stopped."
else
	echo "-- Starting Hadoop daemons"
	start-dfs.sh >& /dev/null
	start-yarn.sh >& /dev/null
	echo "-- Running from config $2"
fi

spark-submit --master yarn "$1" "$2"

if [[ $RES != 0 ]]; then
	echo "-- Stopping Hadoop daemons"
	stop-dfs.sh >& /dev/null
	stop-yarn.sh >& /dev/null
fi

echo "-- Done"