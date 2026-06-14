import json
import glob

files = glob.glob('Baza*.txt')
fname = files[0]
print('Plik:', fname)

with open(fname, 'r', encoding='utf-8-sig') as f:
    data = json.load(f)

features = data['features']
print('Liczba kodow QR:', len(features))

floors = {}
for feat in features:
    level = feat['attributes']['poziom']
    floors.setdefault(level, []).append(feat['attributes']['qr_text'])

for lvl in sorted(floors.keys()):
    print('Pietro', lvl, ':', len(floors[lvl]), 'kodow')

print()
print('Przyklad (pierwsze 5):')
for feat in features[:5]:
    a = feat['attributes']
    g = feat['geometry']
    qr = a['qr_text']
    lvl = a['poziom']
    x = g['x']
    y = g['y']
    print(' ', qr, '| pietro=', lvl, '| x=', round(x,1), 'y=', round(y,1))

# Generate Kotlin offline database
print()
print('Generuje plik Kotlin...')
lines = []
lines.append('package com.example.szpital.data.local')
lines.append('')
lines.append('// Offline baza kodow QR - Gmach Glowny PW (building_id=39)')
lines.append('// Wspolrzedne w EPSG:2180, konwertowane do WGS84 w runtime')
lines.append('data class LocalQrEntry(')
lines.append('    val qrText: String,')
lines.append('    val buildingId: Int,')
lines.append('    val floor: Int,')
lines.append('    val x: Double,  // EPSG:2180')
lines.append('    val y: Double   // EPSG:2180')
lines.append(')')
lines.append('')
lines.append('object LocalQrDatabase {')
lines.append('    val entries = listOf(')

for feat in features:
    a = feat['attributes']
    g = feat['geometry']
    qr = a['qr_text']
    bid = a['building_id']
    lvl = a['poziom']
    x = g['x']
    y = g['y']
    lines.append('        LocalQrEntry("%s", %d, %d, %.5f, %.5f),' % (qr, bid, lvl, x, y))

lines.append('    )')
lines.append('')
lines.append('    fun findByQrText(qrText: String): LocalQrEntry? =')
lines.append('        entries.find { it.qrText.equals(qrText.trim(), ignoreCase = true) }')
lines.append('}')

with open('LocalQrDatabase.kt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print('Zapisano LocalQrDatabase.kt z', len(features), 'wpisami')
