import glob, re

updated = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    
    # Use a looser regex
    pattern = re.compile(r'<script>\s*document\.getElementById\([\'"]sidebarToggle[\'"]\)\.addEventListener.*?\}\);\s*</script>', re.DOTALL)
    nc = pattern.sub('', c)
    if nc != c:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(nc)
        updated += 1

print('Removed duplicate listener from remaining', updated, 'files')
