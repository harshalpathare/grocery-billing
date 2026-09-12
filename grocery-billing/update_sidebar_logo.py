import glob

def update_sidebar_brand_logo():
    files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
    count = 0
    
    target = '<div class="sidebar-brand"><i th:class="\'bi \' + ${globalAppIcon}"></i><span th:text="${globalAppName}">Grocery Bill</span></div>'
    replacement = '''    <div class="sidebar-brand">
        <img th:if="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:src="${globalLogoUrl}" alt="Logo" style="width: 32px; height: 32px; border-radius: 8px; object-fit: cover;">
        <i th:unless="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:class="'bi ' + ${globalAppIcon}"></i>
        <span th:text="${globalAppName}">Grocery Bill</span>
    </div>'''
    
    for file in files:
        if 'base.html' in file or 'settings.html' in file:
            continue
            
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if target in content:
            new_content = content.replace(target, replacement)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1
            print(f'Updated {file}')
            
    print(f'Total files updated: {count}')

update_sidebar_brand_logo()
