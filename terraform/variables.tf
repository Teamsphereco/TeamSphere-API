variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "bucket_name" {
  description = "TeamSphere AWS S3 bucket"
  type        = string
}

variable "app_name" {
  description = "TeamSphere AWS S3 bucket"
  type        = string
  default     = "TeamSphere"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}