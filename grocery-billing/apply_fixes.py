import glob
import re

def update_templates():
    files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
    count = 0
    
    for file in files:
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 1. Update Sidebar Color
        target_color = '<span th:text="${globalAppName}">Grocery Bill</span>'
        replacement_color = '<span th:text="${globalAppName}" th:style="\'color: \' + ${globalAppNameColor} + \';\'">Grocery Bill</span>'
        if target_color in content:
            content = content.replace(target_color, replacement_color)
            
        # 2. Update Sidebar Logo
        target_logo_1 = '<i class="bi bi-shop fs-4 text-primary"></i>'
        target_logo_2 = '<i th:class="\'bi \' + ${globalAppIcon}"></i>'
        replacement_logo = '''<img th:if="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:src="${globalLogoUrl}" alt="Logo" style="width: 32px; height: 32px; border-radius: 8px; object-fit: cover;">
        <i th:unless="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:class="'bi ' + ${globalAppIcon}"></i>'''
        if target_logo_1 in content:
            content = content.replace(target_logo_1, replacement_logo)
        elif target_logo_2 in content:
            content = content.replace(target_logo_2, replacement_logo)
            
        # 3. Add Hamburger Menu
        if '<nav class="top-navbar">' in content and 'sidebarToggle' not in content:
            if 'class="sidebar"' in content or 'th:replace="layout/base :: sidebar"' in content or 'class="main-content"' in content:
                pattern = r'(<nav class="top-navbar">)'
                replacement = r'\1\n        <button class="btn btn-sm btn-outline-secondary" id="sidebarToggle"><i class="bi bi-list"></i></button>'
                content = re.sub(pattern, replacement, content, count=1)
                
        # 4. Super Admin Topbar Settings Icon
        if 'hasRole(\'\'SUPER_ADMIN\'\')' in content and 'top-navbar' in content:
            if 'btn-outline-warning' not in content:
                # Add settings icon to super admin dropdown/topbar area
                pattern_super = r'(<div class="ms-auto d-flex align-items-center gap-2">)'
                replacement_super = r'\1\n            <!-- Super Admin Top Settings Icon -->\n            <a th:href="@{/super/settings}" class="btn btn-sm btn-outline-warning" th:if="${#authorization.expression(\'hasRole(\'\'SUPER_ADMIN\'\')\')}">\n                <i class="bi bi-gear-fill"></i>\n            </a>'
                content = re.sub(pattern_super, replacement_super, content, count=1)
                
        # 5. Fix bill/quick.html missing table-responsive
        if 'bill/quick.html' in file.replace('\\', '/'):
            # Manual regex replacement for tables not already in .table-responsive
            # Since this is tricky in python regex, let's just find <table class="table and check if previous line has table-responsive
            if 'table-responsive' not in content:
                content = re.sub(r'(<table[^>]*>)', r'<div class="table-responsive">\n                \1', content)
                content = content.replace('</table>', '</table>\n            </div>')
                
        # 6. Bump cache version to v=26
        content = content.replace('v=25', 'v=26')
        content = content.replace('v=8', 'v=9') # For css just in case

        if content != original_content:
            with open(file, 'w', encoding='utf-8') as f:
                f.write(content)
            count += 1
            
    print(f"Updated {count} template files successfully.")

update_templates()
