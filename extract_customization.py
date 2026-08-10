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

print(f"Start: {start_idx}, End: {end_idx}")

if start_idx != -1 and end_idx != -1:
    extracted = lines[start_idx:end_idx]
    with open("customization_ui.kt", "w") as f:
        f.writelines(extracted)
    
    # Let's not modify ProfileScreen.kt via script yet, I'll use multi_edit_file
