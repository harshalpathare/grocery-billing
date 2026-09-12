import glob

def update_sidebar_brand_color():
    files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
    count = 0
    
    target = '<span th:text="${globalAppName}">Grocery Bill</span>'
    replacement = '<span th:text="${globalAppName}" th:style="\'color: \' + ${globalAppNameColor} + \';\'">Grocery Bill</span>'
    
    for file in files:
        if 'settings.html' in file:
            # wait, settings.html has the sidebar too, let it update unless it's the super settings that doesn't have sidebar.
            # actually super settings has the layout? No, super pages have topbar only.
            pass
            
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if target in content:
            new_content = content.replace(target, replacement)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1
            print(f'Updated {file}')
            
    print(f'Total files updated: {count}')

update_sidebar_brand_color()
