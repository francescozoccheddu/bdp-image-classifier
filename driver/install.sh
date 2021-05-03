# Spark setup

# Install OpenJDK 11
sudo apt update
sudo apt install openjdk-11-jdk -y

# Install Scala 2.12.13
wget https://downloads.lightbend.com/scala/2.12.13/scala-2.12.13.deb -O .scala.deb -nv --show-progress
sudo dpkg -i .scala.deb
rm -f .scala.deb

# Install Spark 3.1.1 for Hadoop 2.7
wget https://downloads.apache.org/spark/spark-3.1.1/spark-3.1.1-bin-hadoop2.7.tgz -O .spark.tgz -nv --show-progress
mkdir .spark
tar xf .spark.tgz -C .spark
rm -f .spark.tgz
sudo mv .spark/* /opt/spark
rm -rf .spark
echo "export SPARK_HOME=/opt/spark" >> ~/.profile
echo "export PATH=\$PATH:\$SPARK_HOME/bin:\$SPARK_HOME/sbin" >> ~/.profile
source ~/.profile
