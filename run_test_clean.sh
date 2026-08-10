#!/bin/bash
export JAVA_TOOL_OPTIONS="-Xmx4g"
export GRADLE_OPTS="-Xmx2g -Dorg.gradle.jvmargs='-Xmx4g -XX:MaxMetaspaceSize=1g'"
./gradlew testDebugUnitTest --tests "com.example.data.database.DatabaseMigrationTest.testMigration34To35" --no-daemon --no-build-cache --rerun-tasks
