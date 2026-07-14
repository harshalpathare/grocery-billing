import glob, re

updated = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = re.sub(r'style\.css\?v=\d+', 'style.css?v=6', content)
    
    if new_content != content:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(new_content)
        updated += 1

print("Total updated:", updated)
