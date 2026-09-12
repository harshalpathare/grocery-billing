import glob

def update_super_admin_topbar():
    files = glob.glob('src/main/resources/templates/super/*.html')
    count = 0
    
    target = '''<a th:href="@{/}" class="btn btn-sm btn-outline-light">
            <i class="bi bi-house me-1"></i>Dashboard
        </a>'''
        
    replacement = '''<a th:href="@{/super/settings}" class="btn btn-sm btn-outline-warning">
            <i class="bi bi-gear-fill me-1"></i>Settings
        </a>
        <a th:href="@{/}" class="btn btn-sm btn-outline-light">
            <i class="bi bi-house me-1"></i>Dashboard
        </a>'''
    
    for file in files:
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if target in content and 'bi-gear-fill me-1' not in content:
            new_content = content.replace(target, replacement)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1
            print(f'Updated {file}')
            
    print(f'Total files updated: {count}')

update_super_admin_topbar()
