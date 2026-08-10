import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

# I truncated ChatViewModel by accident when I did:
# idx = content.find("    fun saveSticker(") 
# This might have deleted everything in the file after the first occurrence, which apparently included all of ChatViewModel except ChatUiState! Let's check git.

import subprocess
out = subprocess.check_output(['git', 'checkout', 'app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt'])
