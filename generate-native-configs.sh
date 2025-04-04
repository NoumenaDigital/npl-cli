#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Building fat jar..."
mvn clean package -DskipTests -PbuildFatjar

echo "Generating native image configurations..."
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -cp target/language-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  com.noumenadigital.npl.lang.server.MainKt
