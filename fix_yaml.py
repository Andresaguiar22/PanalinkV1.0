import sys

with open('.github/workflows/panalink-pipeline.yml', 'r') as f:
    content = f.read()

bad_build_step = """      - name: Build Release APK
        run: gradle assembleRelease --no-daemon"""

good_build_step = """      - name: Build Release APK
        env:
          KEYSTORE_FILE: release-key.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: gradle assembleRelease --no-daemon"""

content = content.replace(bad_build_step, good_build_step)

with open('.github/workflows/panalink-pipeline.yml', 'w') as f:
    f.write(content)

