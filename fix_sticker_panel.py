import re

with open("app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt", "r") as f:
    content = f.read()

if "import com.example.features.stickers.editor.StickerEditorScreen" not in content:
    content = content.replace("import com.example.features.stickers.domain.StickerPack", "import com.example.features.stickers.domain.StickerPack\nimport com.example.features.stickers.editor.StickerEditorScreen\nimport androidx.compose.ui.window.Dialog\nimport androidx.compose.ui.window.DialogProperties")

if "var showEditor by remember { mutableStateOf(false) }" not in content:
    content = content.replace("var isLoading by remember { mutableStateOf(true) }", "var isLoading by remember { mutableStateOf(true) }\n    var showEditor by remember { mutableStateOf(false) }")

    # Add the dialog to the bottom of the function
    dialog_code = """
    if (showEditor) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            StickerEditorScreen(
                onBack = { showEditor = false },
                onStickerCreated = { url ->
                    showEditor = false
                    // Update recent/saved implicitly by re-fetching
                    launch(Dispatchers.IO) {
                        updatePacksUI()
                    }
                }
            )
        }
    }
"""
    # Insert it right before the last closing brace
    content = content[:content.rfind("}")] + dialog_code + "}\n"

# Now add a "+" button in the packs row
# The packs row is a LazyRow
lazy_row = """LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {"""
replacement_lazy_row = """LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
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
                }
"""
content = content.replace(lazy_row, replacement_lazy_row)

with open("app/src/main/java/com/example/features/stickers/presentation/StickerPanel.kt", "w") as f:
    f.write(content)
