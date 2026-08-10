with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

braces = 0
for i, line in enumerate(lines, 1):
    if "fun LegacyProfileScreen" in line:
        braces = 0
    braces += line.count('{')
    braces -= line.count('}')
    if braces < 0:
        print(f"Braces went negative at line {i}: {line.strip()}")
        break
