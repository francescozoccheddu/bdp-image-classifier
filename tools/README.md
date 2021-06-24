# Tools for bdp-image-classifier

A set of handy [Python](https://www.python.org/) tools for building, debugging, testing and deploying the classifier app.

## Installation

> **NOTE:**  
> You need [Python](https://www.python.org/) 3.7 or later and [pip](https://github.com/pypa/pip).  

Simply run
<pre lang="bash">
pip install https://github.com/francescozoccheddu/bdp-image-classifier/releases/download/latest/bdp_image_classifier-0.1-py3-none-any.whl
</pre>

It will install a `bdp-image-classifier` [pip](https://github.com/pypa/pip) package and the following commands:

- `bdp-ic-app-assembly`
- `bdp-ic-app-download`
- `bdp-ic-datasets-download`
- `bdp-ic-datasets-reconfigure`
- `bdp-ic-debug-env-install`
- `bdp-ic-debug-env-run`
- `bdp-ic-debug-env-uninstall`
- `bdp-ic-emr-run`
- `bdp-ic-emr-run-test`

Run <code lang="bash"><i>COMMAND</i> --help</code> for help.

## Examples

To run the classifier on a local [Spark](https://spark.apache.org/) installation, see [EXAMPLE_LOCAL.md](EXAMPLE_LOCAL.md).

To run the classifier on a [AWS EMR](https://aws.amazon.com/emr/) cluster, see [EXAMPLE_EMR.md](EXAMPLE_EMR.md).