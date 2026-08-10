import urllib.request
import json
import os

url = "https://tivqjfgjdxgzicrridaz.supabase.co/rest/v1/stickers"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpdnFqZmdqZHhnemljcnJpZGF6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNDA2NzAsImV4cCI6MjA5NzcxNjY3MH0.vvBHFJiWHGhpAVeY5LPWT7rQincxfqzPBNaf8mFAfds"
req = urllib.request.Request(url, headers={'apikey': key, 'Authorization': f'Bearer {key}', 'Content-Type': 'application/json', 'Prefer': 'return=representation'}, method='POST')
for col in ["id", "name", "image_url", "thumbnail_url", "emoji", "media_type", "owner_id", "pack_id"]:
    try:
        urllib.request.urlopen(req, data=json.dumps({col: "test"}).encode())
    except urllib.error.HTTPError as e:
        err = e.read().decode()
        if 'Could not find the' in err:
            print(f"{col} DOES NOT EXIST")
