locals {
  claims = ["party", "position", "department"] // list of supported claims
}

resource "keycloak_realm" "noumena" {
  realm = "noumena"
}

resource "keycloak_openid_client" "client" {
  realm_id                     = keycloak_realm.noumena.id
  client_id                    = "nm-platform-service-client"
  client_secret                = "87ff12ca-cf29-4719-bda8-c92faa78e3c4"
  access_type                  = "CONFIDENTIAL"
  direct_access_grants_enabled = true
  web_origins                  = ["*"]
}

// protocol mappers take care of converting user attributes to JWT claims
// for convenience, we use the same name for user attributes and claims
resource "keycloak_openid_user_attribute_protocol_mapper" "protocol_mapper" {
  for_each = toset(local.claims)

  realm_id         = keycloak_realm.noumena.id
  client_id        = keycloak_openid_client.client.id
  name             = "${each.key}-mapper"
  user_attribute   = each.key
  claim_name       = each.key
  claim_value_type = "JSON"
}

resource "keycloak_user" "user1" {
  realm_id = keycloak_realm.noumena.id
  username = "user1"
  initial_password {
    value     = "password1"
    temporary = false
  }
  attributes = {
    party = jsonencode(["party1", "party2"])
  }
}

resource "keycloak_user" "user2" {
  realm_id = keycloak_realm.noumena.id
  username = "user2"
  initial_password {
    value     = "password2"
    temporary = false
  }
  attributes = {
    department = jsonencode(["it", "engineering"])
    position   = jsonencode(["developer", "manager"])
  }
}

resource "keycloak_user" "user3" {
  realm_id = keycloak_realm.noumena.id
  username = "user3"
  initial_password {
    value     = "password3"
    temporary = false
  }
  attributes = {
    department = jsonencode(["it", "engineering"])
    position   = jsonencode(["ceo"])
  }
}
