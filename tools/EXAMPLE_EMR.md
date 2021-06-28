# Run the classifier on a [AWS EMR](https://aws.amazon.com/emr/) cluster

> **USE AT YOUR OWN RISK.**  
> [AWS](https://aws.amazon.com/) will charge you for the [EMR](https://aws.amazon.com/emr/) cluster and the [S3](https://aws.amazon.com/s3/) bucket.  
> The script should automatically terminate all allocated resources on exit, but bugs happen.  
> Carefully check the script output and make sure that no [AWS](https://aws.amazon.com/) resources are active after its termination (by checking on the [AWS console](https://console.aws.amazon.com/), for instance).  
> I am not responsible for any unexpected charge.

> **NOTE:**  
> See also [terraform-emr/](../terraform-emr/) if you prefer using [Terraform](https://www.terraform.io/).

## Run with a preconfigured test dataset

Simply run

<pre lang="bash">
export AWS_ACCESS_KEY_ID="<i>YOUR_ACCESS_KEY_ID</i>"
export AWS_SECRET_ACCESS_KEY="<i>YOUR_SECRET_ACCESS_KEY</i>"
bdp-ic-emr-run-test <i>DATASET_NAME</i>
</pre>

> **NOTE:**   
> To create an [AWS](https://aws.amazon.com/) access key
> 1. Log into your [AWS](https://aws.amazon.com/) account
> 2. Go into "[My Security Credentials](https://console.aws.amazon.com/iam/home#/security_credentials)" (from the dropdown menu next your username, on the upper right corner)
> 3. Select "Create New Access Key"
>  
> Be sure to copy or download the secret access key before closing the popup, as you cannot retrieve it later.

> **NOTE:**   
> See [DATASETS.md](DATASETS.md) to choose a valid <code><i>DATASET_NAME</i></code>.

> **NOTE:**   
> You may want to change the default [EC2](https://aws.amazon.com/ec2/) instance count and type and the [AWS](https://aws.amazon.com/) region to use.  
> Use the `--help` flag for help.

## Run with a custom dataset

The procedure is similar, but you have to pass the file path of a *configuration generator script* as argument to `bdp-ic-emr-run`.  
A configuration generator script is any kind of executable compatible with [Amazon Linux 2](https://aws.amazon.com/amazon-linux-2/) (such as a [Bash](https://www.gnu.org/software/bash/) script) that does the necessary operations to retrieve a dataset and outputs its configuration as a JSON string to the standard output.  
Such a script can, for instance, download a dataset from the Internet or from a [S3](https://aws.amazon.com/s3/) bucket and print its configuration.  
Run <code lang="bash">bdp-ic-emr-run --help</code> for help.