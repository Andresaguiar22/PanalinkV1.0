import re

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'var profileThemeChoice by remember' in line or \
       'bottomBarColorChoice' in line or \
       'bottomBarShapeChoice' in line or \
       'var customR by remember' in line or \
       'var customG by remember' in line or \
       'var customB by remember' in line or \
       'var customSecR by remember' in line or \
       'var customSecG by remember' in line or \
       'var customSecB by remember' in line or \
       'val activeMinimalistMode by' in line or \
       'ThemeManager.customSecondary.value = newColor' in line:
        print(f"{i+1}: {line.strip()}")
