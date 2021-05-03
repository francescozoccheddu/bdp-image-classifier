# Spark setup

# Uninstall OpenJDK 11
sudo apt purge openjdk-11-jdk -y

# Uninstall Scala 2.12.13
sudo apt purge scala -y

# Uninstall Spark 3.1.1 for Hadoop 2.7
sudo rm -rf /opt/spark
sed -i '/SPARK_HOME/d' ~/.profile  
source ~/.profile

# Cleanup
sudo apt autoremove -y
