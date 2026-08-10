with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()
with open("notif_block.txt", "w") as f2:
    for i in range(1536, min(2020, len(lines))):
        f2.write(lines[i-1])
