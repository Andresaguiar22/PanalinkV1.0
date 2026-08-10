with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    text = f.read()

text = text.replace("val themeGradient = remember(profileThemeChoice, customPState, customSState) {",
"""val themeKey by com.example.ui.theme.ThemeManager.themeKey.collectAsState()
            val themeGradient = remember(themeKey, customPState, customSState) {""")

text = text.replace("when (profileThemeChoice) {", "when (themeKey) {")

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.write(text)
