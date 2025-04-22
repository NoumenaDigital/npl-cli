#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Building fat jar..."
mvn clean package -DskipTests -PbuildFatjar

echo "Generating native image configurations..."

commands=("help" "version" "check" "openapi" " " "verstion" "asdsadsa")

for cmd in "${commands[@]}"; do
  echo "Running command: $cmd"
  java -agentlib:native-image-agent=config-merge-dir=npl-cli-core/src/main/resources/META-INF/native-image \
    -cp npl-cli-core/target/npl-cli-jar-with-dependencies.jar \
    com.noumenadigital.npl.cli.MainKt "$cmd"
done
