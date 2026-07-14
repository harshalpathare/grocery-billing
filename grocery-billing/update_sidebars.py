import os, glob

search_text = '        <a th:href="@{/reports}" class="nav-link">'
insert_text = '        <a th:href="@{/cashflow}" class="nav-link" th:if="${features[\'enable_cash_flow\'] == true}">\n            <i class="bi bi-wallet2 text-info"></i><span>Cash Flow</span>\n        </a>\n'

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if search_text in content and 'enable_cash_flow' not in content:
        content = content.replace(search_text, insert_text + search_text)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        count += 1
print(f'Updated {count} files.')
