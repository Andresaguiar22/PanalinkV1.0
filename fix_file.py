with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    text = f.read()

text = text.replace("""        com.example.ui.theme.ThemeManager.bottomBarColorPreset.value = bottomBarColorChoice
        com.example.ui.theme.ThemeManager.bottomBarShapePreset.value = bottomBarShapeChoice
    }
""", "")

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.write(text)
