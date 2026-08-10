import re

with open('app/src/main/java/com/example/data/repository/StickerRepository.kt', 'r') as f:
    content = f.read()

content = content.replace("SupabaseClient.currentUser?.token", "SupabaseClient.currentToken")
content = content.replace("StickerResult(it.url, \"\", it.url, null, \"recent\")", "Sticker(it.url, \"\", it.url, null, \"recent\")")

# Also the error: No value passed for parameter 'url', No value passed for parameter 'preview' in StickerResult
# The constructor is `StickerResult(url = ..., preview = ...)`
# Let's fix parseStickerResultList where it does `StickerResult(url, preview!!)`
content = content.replace("StickerResult(url, preview!!)", "StickerResult(url = url, preview = preview!!)")

with open('app/src/main/java/com/example/data/repository/StickerRepository.kt', 'w') as f:
    f.write(content)
print("Done fixing token and StickerResult")
