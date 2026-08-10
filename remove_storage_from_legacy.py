with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    content = f.read()

# 1. Remove states
old_states = """    // Logout Premium states
    var showLogoutPremiumDialog by remember { mutableStateOf(false) }
    var cacheClearingAnimationActive by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf(0f) }"""

content = content.replace(old_states, "")

# 2. Remove Logout Button block
old_button = """            Spacer(modifier = Modifier.height(16.dp))
            // Global Logout Button triggering the Premium Safe mode logout dialog
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showLogoutPremiumDialog = true 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Cerrar Sesión de Pana 🇻🇪",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }"""

content = content.replace(old_button, "")

# 3. Remove Dialog block
dialog_start = "    // High-Fidelity Premium Logout dialog with Cache Sweeping loops"
dialog_end = "        )\n    }\n}"

if dialog_start in content:
    idx_start = content.find(dialog_start)
    # find where this dialog block ends
    idx_end = content.find("        )\n    }\n}\n\n// Reusable Sub-settings", idx_start)
    if idx_end != -1:
        content = content[:idx_start] + "}\n\n// Reusable Sub-settings" + content[idx_end + len("        )\n    }\n}\n\n// Reusable Sub-settings"):]

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.write(content)

