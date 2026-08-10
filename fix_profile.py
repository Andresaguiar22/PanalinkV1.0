import sys

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    com.example.ui.settings.navigation.SettingsNavGraph(onBackToMain = onBack)\n    if (false) {\n", "")
# Since I never appended the closing brace, we don't need to remove it!

# Now rename the old ProfileScreen
content = content.replace("fun ProfileScreen(", "fun LegacyProfileScreen(")

# Add the new ProfileScreen
new_profile_screen = """
@Composable
fun ProfileScreen(
    viewModel: com.example.ui.viewmodel.ProfileViewModel,
    authViewModel: com.example.ui.viewmodel.AuthViewModel,
    onBack: () -> Unit,
    onNavigateToReel: (String) -> Unit = {}
) {
    com.example.ui.settings.navigation.SettingsNavGraph(onBackToMain = onBack)
}
"""
content += new_profile_screen

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.write(content)
