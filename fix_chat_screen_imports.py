with open('app/src/main/java/com/example/ui/screen/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add import if not present
if "import com.example.ui.viewmodel.ChatViewModel" not in content:
    content = content.replace("import com.example.ui.viewmodel.ChannelViewModel", "import com.example.ui.viewmodel.ChannelViewModel\nimport com.example.ui.viewmodel.ChatViewModel")

with open('app/src/main/java/com/example/ui/screen/ChatScreen.kt', 'w') as f:
    f.write(content)
