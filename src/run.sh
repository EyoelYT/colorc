#!/bin/bash
# Thin wrapper kept for habit/muscle memory. The Gradle build is the source of
# truth: it pulls JavaFX itself, so there's no manual SDK path
# or classpath to maintain here.
cd "$(dirname "$0")/.." && exec ./gradlew run "$@"
