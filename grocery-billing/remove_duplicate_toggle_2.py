import glob, re

updated = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    
    # We want to remove the specific event listener block.
    # It might have a comment like // Sidebar toggle
    pattern = re.compile(r'(//\s*Sidebar toggle\s*)?document\.getElementById\([\'"]sidebarToggle[\'"]\)\.addEventListener\([\'"]click[\'"], function \(\) \{\s*document\.getElementById\([\'"]sidebar[\'"]\)\.classList\.toggle\([\'"]collapsed[\'"]\);\s*document\.getElementById\([\'"]mainContent[\'"]\)\.classList\.toggle\([\'"]expanded[\'"]\);\s*\}\);', re.DOTALL)
    
    nc = pattern.sub('', c)
    
    # also clean up empty script tags if left behind
    nc = re.sub(r'<script>\s*</script>', '', nc)
    
    if nc != c:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(nc)
        updated += 1
        print('Fixed:', fp)

print('Removed duplicate listener from remaining', updated, 'files')
