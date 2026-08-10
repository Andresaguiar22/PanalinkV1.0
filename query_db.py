import urllib.request
import json
import re

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
req = urllib.request.Request(f"{url}/rest/v1/messages?select=message_type&limit=50")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
try:
    response = urllib.request.urlopen(req)
    print("messages:", response.read().decode())
except Exception as e:
    print("Error messages:", e.read().decode())
    
req = urllib.request.Request(f"{url}/rest/v1/thread_messages?select=message_type&limit=50")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
try:
    response = urllib.request.urlopen(req)
    print("thread_messages:", response.read().decode())
except Exception as e:
    print("Error thread_messages:", e.read().decode())
