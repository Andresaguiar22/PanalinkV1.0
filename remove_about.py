import sys

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines, 1):
    if 2214 <= i <= 2319:
        continue
    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.writelines(new_lines)
