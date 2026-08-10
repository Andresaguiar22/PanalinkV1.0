with open("app/src/main/java/com/example/ui/screen/ChatScreen.kt", "r") as f:
    lines = f.readlines()

with open("app/src/main/java/com/example/ui/screen/ChatScreen.kt", "w") as f:
    skip = False
    for line in lines:
        if "onSaveSticker = { url -> viewModel.saveSticker(url, context) }," in line:
            if skip:
                continue
            skip = True
        elif "onToggleStickerFavorite = { url -> viewModel.toggleStickerFavorite(url, context) }," in line:
            # We already started skipping, so this is the second line of the duplicate block
            if skip and "isEdited =" not in line:
                continue
        elif "isEdited = message.isEdited" in line:
            skip = False # reset after we've skipped the duplicates
        
        if "audioCurrentPositionMs = previewPlayerState.currentPositionMs.toInt()" in line:
            line = line.replace(".toInt()", "")
            
        f.write(line)
