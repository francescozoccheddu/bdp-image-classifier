# Spark uninstaller

# Uninstall OpenJDK 8
sudo apt purge openjdk-8-jdk -y

# Uninstall Scala
sudo apt purge scala -y

# Uninstall Spark
sudo rm -rf /opt/spark
sed -i '/SPARK_HOME/d' ~/.profile  
source ~/.profile

# Cleanup
sudo apt autoremove -y
