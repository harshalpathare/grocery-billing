import glob

def update_other_super_admin_topbars():
    files = ['src/main/resources/templates/super/shop-form.html', 
             'src/main/resources/templates/super/shop-users.html', 
             'src/main/resources/templates/super/shop-features.html']
    count = 0
    
    target_start = '<div class="topbar">'
    target_end = '</div>'
    
    replacement_buttons = '''    <div style="margin-left:auto;display:flex;gap:10px;">
        <a th:href="@{/super/settings}" class="btn btn-sm btn-outline-warning">
            <i class="bi bi-gear-fill me-1"></i>Settings
        </a>
        <a th:href="@{/super/shops}" class="btn btn-sm btn-outline-light">
            <i class="bi bi-shop me-1"></i>All Shops
        </a>
        <a th:href="@{/}" class="btn btn-sm btn-outline-light">
            <i class="bi bi-house me-1"></i>Dashboard
        </a>
    </div>
'''
    
    for file in files:
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if 'margin-left:auto' not in content:
            idx1 = content.find(target_start)
            idx2 = content.find(target_end, idx1)
            
            if idx1 != -1 and idx2 != -1:
                new_content = content[:idx2] + replacement_buttons + content[idx2:]
                with open(file, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated {file}')
            
    print(f'Total files updated: {count}')

update_other_super_admin_topbars()
