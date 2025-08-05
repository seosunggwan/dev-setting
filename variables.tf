variable "db_password" {
  description = "RDS root user password"
  type        = string
  sensitive   = true
}

variable "key_name" {
  description = "Name of the AWS key pair"
  type        = string
  default     = null
  sensitive   = true
}

variable "public_key_content" {
  description = "Content of the public key for AWS key pair"
  type        = string
  default     = null
  sensitive   = true
} 