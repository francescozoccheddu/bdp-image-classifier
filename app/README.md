# Classifier app

## Building

Building the app requires [SBT](https://www.scala-sbt.org/). See the [installation page](https://www.scala-sbt.org/download.html) for more information.

To build and assembly the app into a JAR, run <code>assembly.sh *OUTPUT_FILE*</code>.

## Pre-built JAR

You can also use the prebuilt `assembly_x64.jar` for x64 architectures.

## Running

The app takes the JSON config file path as the only parameter.  
Note that the output JAR does not contain Spark dependencies, so it needs to be run via `spark-submit` (or `run.sh` from [`debug-env/`](debug-env/)).
