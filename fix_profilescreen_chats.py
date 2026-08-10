with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip_mode = False
for i, line in enumerate(lines, 1):
    # Remove state variables
    if 'var chatTextSize by remember' in line:
        continue
    if 'var chatEnterSends by remember' in line:
        continue
    if 'var chatWallpaper by remember' in line:
        continue
        
    # Remove from LaunchedEffect arguments
    if 'LaunchedEffect(privacyLastSeen, privacyReadReceipts, chatTextSize, chatEnterSends, chatWallpaper, presenceStatus, profileThemeChoice, advancedInvisibility, smartReadReceipt)' in line:
        line = line.replace('chatTextSize, chatEnterSends, chatWallpaper, ', '')
        new_lines.append(line)
        continue
        
    # Remove putFloat and putBoolean calls in LaunchedEffect
    if 'putFloat("chat_text_size_${currentUid}", chatTextSize)' in line:
        continue
    if 'putBoolean("chat_enter_sends_${currentUid}", chatEnterSends)' in line:
        continue
    if 'putString("chat_wallpaper_${currentUid}", chatWallpaper)' in line:
        continue
        
    # Remove UI block
    if '// --- CATEGORY 4: CHATS Y APARIENCIA (Chats Preferences) ---' in line:
        skip_mode = True
        continue
        
    if skip_mode:
        if '// --- CATEGORY: NOTIFICACIONES (Canales, Tonos y Vibración) ---' in line:
            skip_mode = False
        else:
            continue
            
    new_lines.append(line)
    
with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
    f.writelines(new_lines)
