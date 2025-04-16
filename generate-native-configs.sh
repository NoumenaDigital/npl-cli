#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Building fat jar..."
mvn clean package -DskipTests -PbuildFatjar

echo "Generating native image configurations..."
java -agentlib:native-image-agent=config-merge-dir=npl-cli-core/src/main/resources/META-INF/native-image \
  -cp npl-cli-core/target/npl-cli-jar-with-dependencies.jar \
  com.noumenadigital.npl.cli.MainKt
