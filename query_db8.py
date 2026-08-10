import urllib.request
import json
import uuid
import datetime

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
def try_insert(mtype):
    data = json.dumps({
        "id": str(uuid.uuid4()),
        "thread_id": "00000000-0000-0000-0000-000000000000",
        "sender_id": "00000000-0000-0000-0000-000000000000",
        "content": "[Test]",
        "message_type": mtype
    }).encode('utf-8')
    req = urllib.request.Request(f"{url}/rest/v1/thread_messages", data=data, method="POST")
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
try_insert("audio/mp4")
try_insert("image")
try_insert("document")
try_insert("video")
try_insert("sticker")
try_insert("gif")
try_insert("text")
