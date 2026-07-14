import os, glob, re

insert_text = '        <a th:href="@{/cashflow}" class="nav-link" th:if="${features[\'enable_cash_flow\'] == true}">\n            <i class="bi bi-wallet2 text-info"></i><span>Cash Flow</span>\n        </a>\n'

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'enable_cash_flow' not in content:
        # We need to find the <a> tag for /reports that is part of the sidebar.
        # It usually looks like <a th:href="@{/reports}"...
        # We replace the first occurrence of it.
        # Using a regex to match the start of the a tag
        pattern = r'(^[ \t]*<a th:href="@\{/reports\}")'
        
        # Check if it matches
        if re.search(pattern, content, re.MULTILINE):
            new_content = re.sub(pattern, insert_text + r'\1', content, count=1, flags=re.MULTILINE)
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1

print(f'Updated {count} files.')
