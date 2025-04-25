FROM hashicorp/terraform:1.11

RUN apk add --no-cache curl wget

WORKDIR /terraform
COPY terraform.tf /terraform/
COPY providers.tf /terraform/
COPY docker-entrypoint.sh /terraform/

RUN chmod +x /terraform/docker-entrypoint.sh
RUN terraform init

VOLUME /terraform

ENTRYPOINT ["/terraform/docker-entrypoint.sh"]
