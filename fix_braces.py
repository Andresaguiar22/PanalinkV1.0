with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

# Delete line 2243
del lines[2242]

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.writelines(lines)
