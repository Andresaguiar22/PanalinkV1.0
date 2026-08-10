import re

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    text = f.read()

# 1. Remove profileThemeChoice from var declarations
text = re.sub(r' *var profileThemeChoice by remember \{ mutableStateOf\(prefs\.getString\("profile_theme_\$\{currentUid\}", "dark_teal"\) \?\: "dark_teal"\) \}\n', '', text)

# 2. Remove other states
text = re.sub(r' *// Bottom Bar Dynamic Themes & Shapes\n', '', text)
text = re.sub(r' *var bottomBarColorChoice by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var bottomBarShapeChoice by remember \{[^\n]+\n', '', text)
text = re.sub(r' *// Custom UI Theme Slider values\n', '', text)
text = re.sub(r' *var customR by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var customG by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var customB by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var customSecR by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var customSecG by remember \{[^\n]+\n', '', text)
text = re.sub(r' *var customSecB by remember \{[^\n]+\n', '', text)
text = re.sub(r' *val activeMinimalistMode by com\.example\.ui\.theme\.ThemeManager\.isMinimalistMode\.collectAsState\(\)\n', '', text)

# 3. Clean up LaunchedEffect keys
text = text.replace("privacyLastSeen, privacyReadReceipts, presenceStatus, profileThemeChoice, advancedInvisibility, smartReadReceipt", 
                    "privacyLastSeen, privacyReadReceipts, presenceStatus, advancedInvisibility, smartReadReceipt")

# 4. Remove profileThemeChoice from putString inside LaunchedEffect
text = re.sub(r' *putString\("profile_theme_\$\{currentUid\}", profileThemeChoice\)\n', '', text)
text = re.sub(r' *putString\("profile_theme_global", profileThemeChoice\)\n', '', text)

# 5. Remove ThemeManager.themeKey.value = profileThemeChoice
text = re.sub(r' *com\.example\.ui\.theme\.ThemeManager\.themeKey\.value = profileThemeChoice\n', '', text)

# 6. Remove custom color LaunchedEffects
text = re.sub(r' *LaunchedEffect\(customR, customG, customB\) \{.*?\}\n\n', '', text, flags=re.DOTALL)
text = re.sub(r' *LaunchedEffect\(customSecR, customSecG, customSecB\) \{.*?\}\n\n', '', text, flags=re.DOTALL)
text = re.sub(r' *LaunchedEffect\(bottomBarColorChoice, bottomBarShapeChoice\) \{.*?\}\n', '', text, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.write(text)
