import urllib.request
import json

url = "https://tivqjfgjdxgzicrridaz.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
    
req = urllib.request.Request(f"{url}/rest/v1/")
req.add_header("apikey", key)
req.add_header("Authorization", f"Bearer {key}")
req.add_header("Accept", "application/openapi+json")
try:
    response = urllib.request.urlopen(req)
    spec = json.loads(response.read().decode())
    print("messages:", json.dumps(spec['definitions']['messages']['properties'], indent=2))
    print("thread_messages:", json.dumps(spec['definitions']['thread_messages']['properties'], indent=2))
except Exception as e:
    print(e)
    print(e.read().decode())
