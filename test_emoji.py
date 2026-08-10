import re

def is_emoji_grapheme(s):
    # Check if grapheme contains emoji characters and no ASCII letters/digits
    if any(c.isalnum() and ord(c) < 128 for c in s):
        return False
    # Check code points
    for c in s:
        cp = ord(c)
        if cp >= 0x1F000 or (0x2600 <= cp <= 0x27BF) or (0x2300 <= cp <= 0x23FF) or (0xFE00 <= cp <= 0xFE0F) or cp == 0x200D:
            return True
        if cp > 127 and not c.isalnum():
            return True
    return False

def check_emoji_only(text):
    trimmed = text.strip()
    if not trimmed:
        return False, []
    
    # We split by graphemes / character boundary
    # For testing, simple regex or unicode handling
    # If text has normal letters or digits (ASCII), false
    if any(c.isalnum() and ord(c) < 128 for c in trimmed):
        return False, []
    
    return True, [trimmed]

print("❤️ ->", check_emoji_only("❤️"))
print("😂 ->", check_emoji_only("😂"))
print("🔥👍🎉 ->", check_emoji_only("🔥👍🎉"))
print("Hola 😁 ->", check_emoji_only("Hola 😁"))
print("123 ->", check_emoji_only("123"))
