#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Building fat jar..."
mvn clean package -DskipTests -PbuildFatjar

echo "Generating native image configurations..."
java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image \
  -cp target/npl-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  com.noumenadigital.npl.cli.MainKt
