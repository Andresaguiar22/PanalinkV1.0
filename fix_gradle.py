import sys

with open('.github/workflows/panalink-pipeline.yml', 'r') as f:
    content = f.read()

content = content.replace(
"""      - name: Build Release APK
        run: ./gradlew assembleRelease --no-daemon""",
"""      - name: Build Release APK
        run: gradle assembleRelease --no-daemon"""
)

with open('.github/workflows/panalink-pipeline.yml', 'w') as f:
    f.write(content)
