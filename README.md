# bdp-image-classifier
*Big Data project by [Francesco Zoccheddu](https://www.github.com/francescozoccheddu).*

Simple image classifier based on the Bag-of-Visual-Words model, written in [Scala](https://www.scala-lang.org/) for [Apache Spark](https://spark.apache.org/) 3.1.2.  

See [BENCHMARK.md](BENCHMARK.md) for benchmark results.

## App

The [app](app/) directory contains the [SBT](https://www.scala-sbt.org/) project and the [Scala](https://www.scala-lang.org/) source code of the classifier app.  
See [app/README.md](app/README.md) for more info.

## Tools

The [tools](tools/) directory contains a set of handy [Python](https://www.python.org/) tools for building, debugging, testing and deploying the classifier app.  
See [tools/README.md](tools/README.md) for more info.

## Terraform

The [terraform-emr](terraform-emr/) directory contains a [Terraform](https://www.terraform.io/) module for running the classifier app on an [AWS EMR](https://aws.amazon.com/emr/) cluster.  
See [terraform-emr/README.md](terraform-emr/README.md) for more info.