import urllib.request
import json
import re
import uuid
import datetime

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
# We need a chat_id and sender_id to test insert.
req = urllib.request.Request(f"{url}/rest/v1/messages?select=chat_id,sender_id&limit=1")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
response = urllib.request.urlopen(req)
row = json.loads(response.read().decode())[0]

chat_id = row['chat_id']
sender_id = row['sender_id']

def try_insert(mtype):
    data = json.dumps({
        "id": str(uuid.uuid4()),
        "chat_id": chat_id,
        "sender_id": sender_id,
        "content": "[Test]",
        "created_at": datetime.datetime.utcnow().isoformat() + "Z",
        "message_type": mtype
    }).encode('utf-8')
    req = urllib.request.Request(f"{url}/rest/v1/messages", data=data, method="POST")
    req.add_header("apikey", key)
    req.add_header("Authorization", f"Bearer {key}")
    req.add_header("Content-Type", "application/json")
    req.add_header("Prefer", "return=representation")
    try:
        response = urllib.request.urlopen(req)
        print(f"{mtype}: SUCCESS")
    except Exception as e:
        print(f"{mtype}: FAILED ({e.read().decode()})")

try_insert("audio")
try_insert("voice")
try_insert("audio/mp4")
try_insert("image")
try_insert("image/jpeg")
try_insert("document")
try_insert("file")
try_insert("video")
try_insert("sticker")
try_insert("gif")
