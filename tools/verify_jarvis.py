"""Verify the pinned source against the audit manifest, without executing Jarvis."""
from pathlib import Path
import hashlib, json

root=Path(__file__).resolve().parents[1]
inventory=json.loads((root/'docs/audit/jarvis-inventory.json').read_text(encoding='utf-8'))
snapshot=root/'third_party/jarvis'
for item in inventory['files']:
    data=(snapshot/item['path']).read_bytes()
    digests={hashlib.sha256(data).hexdigest(),hashlib.sha256(data.replace(b'\r\n',b'\n')).hexdigest()}
    assert item['sha256'] in digests,item['path']
license_text=(snapshot/'LICENSE').read_text(encoding='utf-8').replace('\r\n','\n')
assert (root/'android/app/src/main/assets/licenses/jarvis-MIT.txt').read_text(encoding='utf-8').replace('\r\n','\n')==license_text
origin=json.loads((root/'android/app/src/main/assets/jarvis-origin.json').read_text(encoding='utf-8'))
assert origin['revision']==inventory['revision']
print(json.dumps({'revision':inventory['revision'],'verified_upstream_files':len(inventory['files']),'android_license':'identical'},indent=2))
