import glob

def fix_gear_icon():
    files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
    count = 0
    for file in files:
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        target = '<i class="bi bi-gear-fill text-secondary"></i>'
        replacement = '<i class="bi bi-gear-fill"></i>'
        
        if target in content:
            new_content = content.replace(target, replacement)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1
            print(f'Fixed {file}')
    
    print(f'Total files fixed: {count}')

fix_gear_icon()
