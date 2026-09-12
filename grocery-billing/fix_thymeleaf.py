import glob

files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)
count = 0
for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix the broken thymeleaf syntax
    broken = "th:if=\"${#authorization.expression(\\'hasRole(\\'\\'SUPER_ADMIN\\'\\')\\')}\""
    fixed = 'th:if="${#authorization.expression(\'hasRole(\'\'SUPER_ADMIN\'\')\')}"'
    
    if broken in content:
        content = content.replace(broken, fixed)
        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
        count += 1

print(f"Fixed {count} files.")
