terraform {
  required_providers {
    keycloak = {
      source  = "mrparkers/keycloak"
      version = "4.4.0"
    }
  }
}

provider "keycloak" {
  client_id     = "admin-cli"
  username      = var.keycloak_user
  password      = var.keycloak_password
  url           = var.keycloak_url
  base_path     = ""
}

variable "keycloak_url" {
  type        = string
  description = "The URL of the Keycloak server"
  default     = "http://keycloak:11000"
}

variable "keycloak_user" {
  type        = string
  description = "The username for the Keycloak admin"
  default     = "admin"
}

variable "keycloak_password" {
  type        = string
  description = "The password for the Keycloak admin"
  default     = "ThePlatonic1"
}
