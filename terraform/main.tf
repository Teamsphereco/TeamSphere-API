terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.100.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}


resource "aws_s3_bucket" "app_bucket" {
  bucket = var.bucket_name
  tags = {
    Name        = var.bucket_name
    Environment = var.environment
  }
}

# Bucket versioning 
resource "aws_s3_bucket_versioning" "app_bucket_versioning" {
  bucket = aws_s3_bucket.app_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

# server-side encryption 
resource "aws_s3_bucket_server_side_encryption_configuration" "app_bucket_encryption" {
  bucket = aws_s3_bucket.app_bucket.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# S3 Bucket public access block (keeps files private)
resource "aws_s3_bucket_public_access_block" "app_bucket_pab" {
  bucket = aws_s3_bucket.app_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CORS config
resource "aws_s3_bucket_cors_configuration" "app_bucket_cors" {
  bucket = aws_s3_bucket.app_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "DELETE", "HEAD"] # TODO: pull this from vault
    allowed_origins = ["http://localhost:3000", "http://localhost:8080"] # TODO: pull this from vault
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# IAM user 
resource "aws_iam_user" "app_user" {
  name = "${var.app_name}-user"
  
  tags = {
    Name        = "${var.app_name}-user"
    Environment = var.environment
  }
}

# IAM policy 
resource "aws_iam_user_policy" "app_s3_policy" {
  name = "${var.app_name}-s3-policy"
  user = aws_iam_user.app_user.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
          "s3:GetObjectVersion"
        ]
        Resource = [
          aws_s3_bucket.app_bucket.arn,
          "${aws_s3_bucket.app_bucket.arn}/*"
        ]
      }
    ]
  })
}

# Access keys
resource "aws_iam_access_key" "app_user_key" {
  user = aws_iam_user.app_user.name
}

output "bucket_name" {
  value = aws_s3_bucket.app_bucket.id
}

output "bucket_region" {
  value = aws_s3_bucket.app_bucket.region
}

output "access_key_id" {
  value = aws_iam_access_key.app_user_key.id
}

output "secret_access_key" {
  value     = aws_iam_access_key.app_user_key.secret
  sensitive = true
}
