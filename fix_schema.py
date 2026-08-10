import json

def fix_schema(path):
    with open(path, 'r') as f:
        db = json.load(f)
    
    for e in db['database']['entities']:
        if e['tableName'] == 'playlist_songs':
            e['createSql'] = "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, `playlistId` TEXT NOT NULL, `trackId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL DEFAULT 0, `addedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
            for f in e['fields']:
                if f['columnName'] == 'isDirty':
                    f['notNull'] = True
                    f['defaultValue'] = '0'
                    
    with open(path, 'w') as f:
        json.dump(db, f, indent=2)

fix_schema('app/src/main/assets/com.example.data.database.PanalinkDatabase/34.json')
fix_schema('app/schemas/com.example.data.database.PanalinkDatabase/34.json')
fix_schema('app/src/test/assets/com.example.data.database.PanalinkDatabase/34.json')
