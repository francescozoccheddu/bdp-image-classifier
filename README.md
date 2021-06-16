# Big Data Project
*Big Data project by [Francesco Zoccheddu](https://www.github.com/francescozoccheddu).*

Simple image classifier with Bag-of-Visual-Words model, written in [Scala](https://www.scala-lang.org/) for [Apache Spark](https://spark.apache.org/) 3.1.2.

> **NOTE:**   
All of the scripts have been tested in Ubuntu 21.04 on a x64 machine.  
They should work in the most popular x86 or x64 Linux distributions, as they only require standard POSIX utilities, `curl`, `whoami`, `python3`, `pip3`, `unzip`, `tar`, `ssh-keygen`, `realpath`, Bash version 4 and an Internet connection.

> **NOTE:**   
Each script depends on local resources whose paths are expressed relative to the script original location, so **you should not change the file structure** for the scripts to work.

## Components

### Application

To build the classifier app, see the [`app/`](app/) directory and [`app/README.md`](app/README.md).

### Datasets

To download a dataset, see the [`datasets/`](datasets/) directory and [`datasets/README.md`](datasets/README.md).

### Debug environment

To setup a local [Spark](https://spark.apache.org/) installation for debugging purposes only, see the [`debug-env/`](debug-env/) directory and [`debug-env/README.md`](debug-env/README.md).

### Cloud deployment

To test the classifier on an [AWS EMR](https://aws.amazon.com/emr/) cluster, see the [`cloud/`](cloud/) directory and [`cloud/README.md`](cloud/README.md).

## Example

You can try the classifier like this:
```bash
git clone https://github.com/francescozoccheddu/big-data-project.git
cd big-data-project
app/download-x64.sh
datasets/supermarket/download.sh
debug-env/install.sh
env/run.sh assembly-x64.jar dataset/config.json
```
