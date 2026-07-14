import glob, re

updated = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    nc = c.replace('style.css?v=2', 'style.css?v=3')
    # Also version unversioned references
    nc = re.sub(r'(/css/style\.css)"', r'\1?v=3"', nc)
    nc = re.sub(r'(@\{/css/style\.css\})"', r'@{/css/style.css?v=3}"', nc)
    if nc != c:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(nc)
        updated += 1
print('Updated', updated, 'files')
