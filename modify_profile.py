import sys

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
in_ui = False
brace_count = 0

for i, line in enumerate(lines):
    if i == 335 and "Scaffold(" in line:
        new_lines.append("    com.example.ui.settings.navigation.SettingsNavGraph(onBackToMain = onBack)\n")
        new_lines.append("    /* \n")
        new_lines.append("    // LEGACY UI CODE DISABLED FOR PHASE 1 ARCHITECTURE REFACTOR\n")
        new_lines.append(line)
        continue
        
    if i > 335 and i < 2746:
        new_lines.append(line)
        if i == 2745:
            new_lines.append("    */\n")
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.writelines(new_lines)
