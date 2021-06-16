# Cloud deployment

Test the classifier with one of the [provided datasets](../datasets/) on an [AWS EMR](https://aws.amazon.com/emr/) cluster.

> **NOTE:**   
**AWS will charge you** for the EMR clusters and the S3 buckets. 
The script *should* automatically terminate all allocated resources on exit, but bugs happen.
Carefully check the script output and make sure that no AWS resources are active after its termination (by checking on the AWS console, for instance).  
**Use it at your own risk.** I am not responsible for any unexpected charge.

## Run the classifier on a cluster

