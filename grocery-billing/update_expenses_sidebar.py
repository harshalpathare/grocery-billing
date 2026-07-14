import os, glob, re

insert_text = '        <a th:href="@{/expenses}" class="nav-link"><i class="bi bi-cash-coin text-danger"></i><span>Expenses</span></a>\n'

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'href="@{/expenses}"' not in content and 'sidebar-nav' in content:
        # Insert before reports
        pattern = r'(^[ \t]*<a th:href="@\{/reports\}")'
        if re.search(pattern, content, re.MULTILINE):
            new_content = re.sub(pattern, insert_text + r'\1', content, count=1, flags=re.MULTILINE)
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1

print(f'Updated {count} files.')
