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
  job_code               = replace(replace(file(format(local.emr_tools_resource_fmt, "job.sh")), "%CG_SCRIPT_FILE%", local.cg_emr_file), "%RESULTS_FILE%", local.results_emr_file)
  cg_code                = var.cg_file != null ? file(var.cg_file) : replace(file(format(local.emr_tools_resource_fmt, "test-config-gen.sh")), "%DATASET%", var.cg_test_dataset)
}

# Resources

provider "aws" {
  profile = "default"
  region  = "us-west-2"
}

resource "aws_s3_bucket" "bucket" {
  bucket_prefix = "${local.prefix}-"
  acl           = "private"
  force_destroy = true
}

resource "aws_s3_bucket_object" "job" {
  key     = "job_script"
  bucket  = aws_s3_bucket.bucket.id
  content = local.job_code
}

resource "aws_s3_bucket_object" "cg" {
  key     = "cg_script"
  bucket  = aws_s3_bucket.bucket.id
  content = local.cg_code
}
