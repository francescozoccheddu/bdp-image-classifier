# Debug environment

Bash scrips to setup a local [Spark](https://spark.apache.org/) 3.1.2 installation on [Hadoop](https://hadoop.apache.org/) 3.2.2, **for debugging pursposes only**.

## Install the environment

Installs the debug environment into `~/.image-classifier-debug-env`.

Run `install.sh`.

You may also want to run `source ~/.profile` to be able to manually run Spark without restarting the current bash shell.

## Run an application

Run <code>run.sh *ASSEMBLY* *CONFIG*</code>, like this:
```bash
data/supermaket/download.sh dataset
debug-env/run.sh app/assembly.jar dataset/config.json
```

You may want to edit `run.sh` to change the Spark driver memory, which defaults to 4GB.

## Uninstall the environment

Uninstalls the debug environment from `~/.image-classifier-debug-env`.

Run `uninstall.sh`.