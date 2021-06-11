# Debug environment

Bash scrips to setup a local [Spark](https://spark.apache.org/) 3.1.2 installation on [Hadoop](https://hadoop.apache.org/) 3.2.2, **for debugging pursposes only**. 

## Install the environment

Run <code>install.sh *INSTALL_DIR*</code>.

## Run an application

Run <code>*INSTALL_DIR*/run.sh *ASSEMBLY_FILE* *CONFIG_FILE*</code>.

You may want to edit `run.sh` to change the Spark driver memory, which defaults to 4GB.

## Uninstall the environment

Run <code>*INSTALL_DIR*/uninstall.sh</code>.
