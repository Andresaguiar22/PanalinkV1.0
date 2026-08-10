with open("notif_block.txt", "r") as f:
    text = f.read()
print(f"{{ count: {text.count('{')}")
print(f"}} count: {text.count('}')}")
