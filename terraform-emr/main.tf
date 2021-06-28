# Terraform

terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }

  required_version = ">= 0.14.9"
}

# Variables

variable "instance_count" {
  type        = number
  default     = 1
  description = "The number of EC2 instances in the cluster."
  validation {
    condition     = var.instance_count >= 1 && var.instance_count <= 10
    error_message = "The instance_count value must fall in range [1, 10]."
  }
}

variable "instance_type" {
  type        = string
  default     = "m5.xlarge"
  description = "The type of the EC2 instances in the cluster."
  validation {
    condition     = contains(["m4.large", "m5.xlarge", "c5.xlarge"], var.instance_type)
    error_message = "The instance_type value must be one of 'm4.large', 'm5.xlarge' or 'c5.xlarge'."
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "cg_file" {
  type    = string
  default = null
}

variable "cg_test_dataset" {
  type        = string
  default     = "test"
  description = "The preconfigured dataset to test."
  validation {
    condition     = contains(["test", "supermarket", "land", "indoor"], var.cg_test_dataset)
    error_message = "The cg_test_dataset value must be one of 'test', 'supermarket', 'land' or 'indoor'."
  }
}

locals {
  prefix                 = "bdp-image-classifier"
  home_emr_dir           = "/home/hadoop"
  cg_emr_file            = "${local.home_emr_dir}/cg_script"
  results_emr_file       = "${local.home_emr_dir}/results.tgz"
  emr_tools_resource_fmt = "${replace(path.module, "%", "%%")}/../tools/bdp_image_classifier/emr/%s.resource"
}

# Resources

provider "aws" {
  profile = "default"
  region  = var.aws_region
}

resource "aws_s3_bucket" "bucket" {
  bucket_prefix = "${local.prefix}-"
  acl           = "private"
  force_destroy = true
}

resource "aws_s3_bucket_object" "job" {
  key     = "job_script"
  bucket  = aws_s3_bucket.bucket.id
  content = replace(replace(file(format(local.emr_tools_resource_fmt, "job.sh")), "%CG_SCRIPT_FILE%", local.cg_emr_file), "%RESULTS_FILE%", local.results_emr_file)
}

resource "aws_s3_bucket_object" "cg" {
  key     = "cg_script"
  bucket  = aws_s3_bucket.bucket.id
  content = var.cg_file != null ? file(var.cg_file) : replace(file(format(local.emr_tools_resource_fmt, "test-config-gen.sh")), "%DATASET%", var.cg_test_dataset)
}

resource "aws_emr_cluster" "cluster" {
  name                              = "${local.prefix}-cluster"
  release_label                     = "emr-6.3.0"
  applications                      = ["Spark"]
  log_uri                           = "s3://${aws_s3_bucket.bucket.id}/logs"
  keep_job_flow_alive_when_no_steps = false
  ebs_root_volume_size              = 10
  visible_to_all_users              = true

  step {
    name              = "Download config generator script from S3"
    action_on_failure = "TERMINATE_CLUSTER"
    hadoop_jar_step {
      jar  = "command-runner.jar"
      args = ["aws", "s3", "cp", "s3://${aws_s3_bucket_object.cg.bucket}/${aws_s3_bucket_object.cg.key}", local.cg_emr_file]
    }
  }

  step {
    name              = "Run job"
    action_on_failure = "TERMINATE_CLUSTER"
    hadoop_jar_step {
      jar  = "s3://${var.aws_region}.elasticmapreduce/libs/script-runner/script-runner.jar"
      args = ["s3://${aws_s3_bucket_object.job.bucket}/${aws_s3_bucket_object.job.key}"]
    }
  }

  step {
    name              = "Upload results to S3"
    action_on_failure = "TERMINATE_CLUSTER"
    hadoop_jar_step {
      jar  = "command-runner.jar"
      args = ["aws", "s3", "cp", local.results_emr_file, "s3://${aws_s3_bucket.bucket.id}"]
    }
  }

  ec2_attributes {
    instance_profile = aws_iam_instance_profile.ec2.arn
  }

  master_instance_group {
    instance_type = var.instance_type
  }

  core_instance_group {
    instance_count = var.instance_count
    instance_type  = var.instance_type
  }

  configurations_json = file(format(local.emr_tools_resource_fmt, "emr-configuration.json"))
  service_role        = aws_iam_role.service.arn

}

resource "aws_iam_role" "service" {
  name_prefix         = "${local.prefix}-service-"
  assume_role_policy  = file(format(local.emr_tools_resource_fmt, "emr-service-policy.json"))
  managed_policy_arns = ["arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceRole"]
}

resource "aws_iam_role" "ec2" {
  name_prefix         = "${local.prefix}-ec2-"
  assume_role_policy  = file(format(local.emr_tools_resource_fmt, "emr-ec2-policy.json"))
  managed_policy_arns = ["arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceforEC2Role"]
}

resource "aws_iam_instance_profile" "ec2" {
  name = "emr_profile"
  role = aws_iam_role.ec2.name
}
