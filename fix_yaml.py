import sys

with open('.github/workflows/panalink-pipeline.yml', 'r') as f:
    content = f.read()

content = content.replace(
"""on:
  push:
    branches: [ "main", "master", "develop" ]
  tags:
    - 'v*'
  workflow_dispatch:""",
"""on:
  push:
    branches: [ "main", "master", "develop" ]
    tags:
      - 'v*'
  workflow_dispatch:"""
)

with open('.github/workflows/panalink-pipeline.yml', 'w') as f:
    f.write(content)
