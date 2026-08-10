import re

with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "r") as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if 'text = "Tema Visual del Sistema (Motor UI) 🎨:"' in line:
        start_idx = i - 1  # include the Spacer before it
    if start_idx != -1 and 'if (saveState is SaveProfileUiState.Error)' in line:
        end_idx = i - 1  # end before the save button block
        break

if start_idx != -1 and end_idx != -1:
    del lines[start_idx:end_idx]
    with open("app/src/main/java/com/example/ui/screen/ProfileScreen.kt", "w") as f:
        f.writelines(lines)
    print("Removed UI block")
else:
    print("UI block not found!")
