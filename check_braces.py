with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

braces = 0
in_legacy = False
for i, line in enumerate(lines, 1):
    if "fun LegacyProfileScreen" in line:
        in_legacy = True
        
    if in_legacy:
        braces += line.count('{')
        braces -= line.count('}')
        if braces == 0 and '{' in line:
            pass # still on the signature
        elif braces == 0 and '}' in line:
            print(f"LegacyProfileScreen closed at line {i}")
            break
