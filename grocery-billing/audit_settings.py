"""
Audit: show exactly where /settings appears in every sidebar template.
"""
import glob, re

for fp in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    with open(fp, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    if not any('sidebar-nav' in l or 'sidebar-bottom' in l for l in lines):
        continue

    settings_lines = [(i+1, l.strip()) for i, l in enumerate(lines) if '/settings' in l]
    if not settings_lines:
        continue

    name = fp.replace('src\\main\\resources\\templates\\', '').replace('src/main/resources/templates/', '')
    print(name)
    for lineno, text in settings_lines:
        print('  L' + str(lineno) + ': ' + text[:120])
    print()
