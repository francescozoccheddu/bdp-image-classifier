# Debug environment

Bash scrips to setup a local [Spark](https://spark.apache.org/) 3.1.2 installation on [Hadoop](https://hadoop.apache.org/) 3.3.0 with YARN, **for debugging pursposes only**.

Tested on Ubuntu 21.04. 
Requires standard POSIX utilities, `curl`, `tar`, `whoami`, `bash` and an Internet connection.

## Setup SSH

Allows SSH connections to `localhost`. 

Run `setup-ssh.sh` and enter your password when requested.

**This step must only be done once**.

## Install the environment

Installs the debug environment into `~/.image-classifier-debug-env`.

Run `install.sh`.

You may also want to run `source ~/.profile` to be able to manually run Spark without restarting the current bash shell.

## Run an application

Run `run.sh `*`<ASSEMBLY> <CONFIG>`*, like this:
```bash
data/supermaket/download.sh dataset
debug-env/run.sh app/assembly.jar dataset/config.json
```

You may want to edit `run.sh` to change the Spark driver memory, which defaults to 4GB.

## Uninstall the environment

Uninstalls the debug environment from `~/.image-classifier-debug-env`.

Run `uninstall.sh`.