import re

with open("app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt", "r") as f:
    content = f.read()

# Replace LaunchedEffect
launched_effect_start = """    LaunchedEffect(Unit) {
        // Load cache first
        suspend fun updatePacksUI() {"""

new_launched_effect_start = """    val coroutineScope = rememberCoroutineScope()
    val updatePacksUI = suspend {
"""
content = content.replace(launched_effect_start, new_launched_effect_start)

# The end of updatePacksUI is `}`. Let's just do a regex replace
content = content.replace("            if (selectedPackId == null && loadedPacks.isNotEmpty()) {\n                selectedPackId = loadedPacks.first().id\n            }\n        }\n                // Show cached immediately\n        updatePacksUI()\n        isLoading = false", "            if (selectedPackId == null && loadedPacks.isNotEmpty()) {\n                selectedPackId = loadedPacks.first().id\n            }\n    }\n\n    LaunchedEffect(Unit) {\n        // Show cached immediately\n        updatePacksUI()\n        isLoading = false")

# Update dialog's launch
dialog_launch = """                    launch(Dispatchers.IO) {
                        updatePacksUI()
                    }"""
new_dialog_launch = """                    coroutineScope.launch(Dispatchers.IO) {
                        updatePacksUI()
                    }"""
content = content.replace(dialog_launch, new_dialog_launch)

# Add + Crear button
lazy_row_start = """            // Pack selector row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111B21))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {"""

new_lazy_row_start = """            // Pack selector row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111B21))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A3942))
                            .clickable { showEditor = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Crear", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }"""
content = content.replace(lazy_row_start, new_lazy_row_start)

with open("app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt", "w") as f:
    f.write(content)
