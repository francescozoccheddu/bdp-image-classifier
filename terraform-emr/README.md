# Run the classifier on a [AWS EMR](https://aws.amazon.com/emr/) cluster using [Terraform](https://www.terraform.io/)

> **NOTE:**  
> [AWS](https://aws.amazon.com/) will charge you for the [EMR](https://aws.amazon.com/emr/) cluster and the [S3](https://aws.amazon.com/s3/) bucket.  
> I am not responsible for any unexpected charge.

> **NOTE:**  
> See also [tools/EXAMPLE_EMR.md](../tools/EXAMPLE_EMR.md) for a better experience.

## Run with a preconfigured test dataset

Simply run

<pre lang="bash">
cd terraform-emr
export AWS_ACCESS_KEY_ID="<i>YOUR_ACCESS_KEY_ID</i>"
export AWS_SECRET_ACCESS_KEY="<i>YOUR_SECRET_ACCESS_KEY</i>"
terraform init
terraform apply -var="cg_test_dataset=<i>DATASET_NAME</i>"
</pre>

[Terraform](https://www.terraform.io/) will create a <code>bdp-image-classifier-<i>xxx</i></code> [S3](https://aws.amazon.com/s3/) bucket (see the output for the actual name) and a `bdp-image-classifier-cluster` [EMR](https://aws.amazon.com/emr/) cluster.  
The cluster will automatically run the classification job and terminate.
You can monitor its process through the [EMR dashboard](https://console.aws.amazon.com/elasticmapreduce/home).  
Upon termination you will find the compressed results archive at <code>s3://bdp-image-classifier-<i>xxx</i>/results.tgz</code>.

You can destroy all allocated resources by running

<pre lang="bash">
terraform destroy
</pre>

> **NOTE:**  
> To install [Terraform](https://www.terraform.io/) follow the [official installation guide](https://learn.hashicorp.com/tutorials/terraform/install-cli).

> **NOTE:**  
> To create an [AWS](https://aws.amazon.com/) access key
>
> 1. Log into your [AWS](https://aws.amazon.com/) account
> 2. Go into "[My Security Credentials](https://console.aws.amazon.com/iam/home#/security_credentials)" (from the dropdown menu next your username, on the upper right corner)
> 3. Select "Create New Access Key"
>
> Be sure to copy or download the secret access key before closing the popup, as you cannot retrieve it later.

> **NOTE:**  
> See [DATASETS.md](DATASETS.md) to choose a valid <code><i>DATASET_NAME</i></code>.

> **NOTE:**  
> You may want to change the default [EC2](https://aws.amazon.com/ec2/) instance count and type and the [AWS](https://aws.amazon.com/) region to use.  
> You can do it by setting the `instance_count`, `instance_type` and `aws_region` [Terraform](https://www.terraform.io/) variables.

## Run with a custom dataset

The procedure is similar, but you have to set the `cg_file` [Terraform](https://www.terraform.io/) variable to the file path of a _configuration generator script_.  
A configuration generator script is any kind of executable compatible with [Amazon Linux 2](https://aws.amazon.com/amazon-linux-2/) (such as a [Bash](https://www.gnu.org/software/bash/) script) that does the necessary operations to retrieve a dataset and outputs its configuration as a JSON string to the standard output.  
Such a script can, for instance, download a dataset from the Internet or from a [S3](https://aws.amazon.com/s3/) bucket and print its configuration.
