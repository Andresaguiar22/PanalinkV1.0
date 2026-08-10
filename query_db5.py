import urllib.request
import json
import uuid
import datetime

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
req = urllib.request.Request(f"{url}/rest/v1/messages?select=chat_id,sender_id,thread_id&limit=1")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
response = urllib.request.urlopen(req)
row = json.loads(response.read().decode())[0]

chat_id = row['chat_id']
sender_id = row['sender_id']
thread_id = row['thread_id']

def try_insert(mtype):
    data = json.dumps({
        "id": str(uuid.uuid4()),
        "chat_id": chat_id,
        "thread_id": thread_id,
        "sender_id": sender_id,
        "text_content": "[Test]",
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
        err = e.read().decode()
        if "messages_message_type_check" in err:
            print(f"{mtype}: CHECK CONSTRAINT FAILED")
        else:
            print(f"{mtype}: FAILED ({err})")

try_insert("audio")
try_insert("audio/mp4")
try_insert("image")
try_insert("document")
try_insert("video")
try_insert("sticker")
try_insert("gif")
try_insert("text")
