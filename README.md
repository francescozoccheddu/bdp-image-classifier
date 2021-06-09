# Big Data Project
*Big Data project by [Francesco Zoccheddu](https://www.github.com/francescozoccheddu).*

Simple image classifier with Bag-of-Visual-Words model, written in [Scala](https://www.scala-lang.org/) for [Apache Spark 3.1.2](https://spark.apache.org/).

> **NOTE:**   
All of the scripts have been tested in Ubuntu 21.04 on a x64 machine.  
They should work in most popular x86 or x64 Linux distributions, as they only require standard POSIX utilities, `curl`, `whoami`, `python3`, `pip3`, `unzip`, `tar`, `ssh-keygen`, `realpath`, Bash version 4 and an Internet connection.

## Application

To build the classifier app, see the [`app/`](app/) directory and [`app/README.md`](app/README.md).

## Datasets

To download a dataset, see the [`datasets/`](datasets/) directory and [`datasets/README.md`](datasets/README.md).

## Debug environment

To setup a local Spark installation for debugging purposes only, see the [`debug-env/`](debug-env/) directory and [`debug-env/README.md`](debug-env/README.md).

## Cloud deployment

To setup a cloud cluster, see the [`cloud/`](cloud/) directory and [`cloud/README.md`](cloud/README.md).
