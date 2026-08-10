import json
import copy

with open('app/src/main/assets/com.example.data.database.PanalinkDatabase/34.json', 'r') as f:
    db = json.load(f)

db['database']['version'] = 33

for e in db['database']['entities']:
    if e['tableName'] == 'local_messages':
        e['fields'] = [f for f in e['fields'] if f['columnName'] != 'musicPlaylistId']
        e['createSql'] = e['createSql'].replace(", `musicPlaylistId` TEXT", "")
    elif e['tableName'] == 'playlist_songs':
        e['fields'] = [f for f in e['fields'] if f['columnName'] != 'isDirty']
        e['createSql'] = e['createSql'].replace(", `isDirty` INTEGER NOT NULL DEFAULT 0", "")
    elif e['tableName'] == 'audio_tracks':
        e['fields'] = [f for f in e['fields'] if f['columnName'] not in ('genre', 'remoteId', 'lastSyncAt', 'isDirty')]
        e['createSql'] = e['createSql'].replace(", `genre` TEXT NOT NULL", "").replace(", `remoteId` TEXT", "").replace(", `lastSyncAt` INTEGER", "").replace(", `isDirty` INTEGER NOT NULL", "")

with open('app/src/main/assets/com.example.data.database.PanalinkDatabase/33.json', 'w') as f:
    json.dump(db, f, indent=2)

with open('app/schemas/com.example.data.database.PanalinkDatabase/33.json', 'w') as f:
    json.dump(db, f, indent=2)

