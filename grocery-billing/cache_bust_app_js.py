import os, glob

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'th:src="@{/js/app.js?v=9}"' in content:
        new_content = content.replace('th:src="@{/js/app.js?v=9}"', 'th:src="@{/js/app.js?v=10}"')
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += 1
        
print(f"Updated {count} files to cache-bust app.js v10")
