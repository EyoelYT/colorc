@echo off
:: Thin wrapper kept for habit/muscle memory. The Gradle build is the source of
:: truth: it pulls JavaFX and jnativehook itself, so there's no manual SDK path
:: or classpath to maintain here.
cd /d "%~dp0.."
call gradlew.bat run %*
