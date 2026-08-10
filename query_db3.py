import urllib.request
import json

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
req = urllib.request.Request(f"{url}/rest/v1/messages?limit=1")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
response = urllib.request.urlopen(req)
row = json.loads(response.read().decode())[0]
print(list(row.keys()))
