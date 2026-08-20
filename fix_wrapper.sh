#!/bin/bash
# Re-generate the wrapper using a known local gradle if possible, or just download the distribution zip.
# Since we are in an restricted environment, try downloading the jar properly.
URL="https://services.gradle.org/distributions/gradle-8.10-bin.zip"
# Assuming we can't easily run gradle to generate, we have to fix the structure manually.
echo "Manually attempting to fix wrapper..."
# This might be tricky, let's try to see if we can find another wrapper file or just download from official.
# Try to download the wrapper jar specifically from a reliable source.
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.10/gradle/wrapper/gradle-wrapper.jar
