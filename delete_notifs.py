with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines, 1):
    if 133 <= i <= 141:
        continue
    if 1536 <= i <= 2017:
        continue
    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.writelines(new_lines)
