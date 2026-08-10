#!/bin/bash
cat app/build/reports/tests/testDebugUnitTest/com.example.data.database.DatabaseMigrationTest/testMigration34To35.html | grep -a -A 10 "java.lang.IllegalStateException[^<]*" | head -n 10
