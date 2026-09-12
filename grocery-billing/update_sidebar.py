import glob
import re

def update_sidebar_brand():
    files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
    count = 0
    
    pattern1 = re.compile(r'<div class="sidebar-brand">\s*<i class="bi bi-shop"></i>\s*<span>Grocery Bill</span>\s*</div>', re.MULTILINE)
    replacement1 = '<div class="sidebar-brand"><i th:class="\'bi \' + ${globalAppIcon}"></i><span th:text="${globalAppName}">Grocery Bill</span></div>'
    
    for file in files:
        if 'base.html' in file or 'settings.html' in file or 'layout' in file:
            continue
            
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if pattern1.search(content):
            new_content = pattern1.sub(replacement1, content)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1
            print(f'Updated {file}')
            
    print(f'Total files updated: {count}')

update_sidebar_brand()
