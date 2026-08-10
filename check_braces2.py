with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()
braces = 0
for i, line in enumerate(lines, 1):
    braces += line.count('{')
    braces -= line.count('}')
    if "fun ProfileScreen(" in line:
        print(f"At {i} before {line.strip()}: braces = {braces}")
print(f"Final: {braces}")
