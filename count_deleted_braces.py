with open("notif_block.txt", "r") as f:
    lines = f.readlines()
text = "".join(lines[:482]) # 1536 to 2017 is 482 lines
print(f"Deleted {{ count: {text.count('{')}")
print(f"Deleted }} count: {text.count('}')}")
