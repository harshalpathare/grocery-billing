import glob

missing = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    if 'id="sidebar"' in c and 'var target = active.offsetTop' not in c:
        print('Missing:', fp)
        missing += 1
print('Total missing:', missing)
