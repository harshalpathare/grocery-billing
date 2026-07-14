import os, glob, re

new_text = '''
        <!-- Accounting -->
        <a th:href="@{/accounting/dashboard}" class="nav-link" th:if="${features['enable_accounting'] == true}" th:classappend="${activePage == 'accounting' ? 'active' : ''}">
            <i class="bi bi-book-half text-secondary"></i><span>Accounting</span>
        </a>
'''

pattern = re.compile(r'[ \t]*<!-- Accounting -->.*?</a>\n', re.DOTALL)

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<!-- Accounting -->' in content and 'enable_accounting' not in content:
        new_content = re.sub(pattern, new_text, content)
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1

print(f'Made Accounting conditional in {count} files.')
