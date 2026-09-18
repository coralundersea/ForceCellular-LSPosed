#!/bin/sh
# Simplified gradlew
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
