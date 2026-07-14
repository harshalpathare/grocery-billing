import os, glob, re

# First, find what the exact Expenses link looks like.
# Usually it's something like:
# <a th:href="@{/expenses}" class="nav-link">
#     <i class="bi bi-receipt text-danger"></i><span>Expenses</span>
# </a>

# Wait, the user already had an update script for it: `update_expenses_sidebar.py`
# Let's see what it inserted.
# We will use a regex to replace it.

pattern = re.compile(r'[ \t]*<a th:href="@\{/expenses\}".*?>.*?</a>\n', re.DOTALL)

new_text = '''
        <a th:href="@{/expenses}" class="nav-link" th:if="${features['enable_expenses'] == true}">
            <i class="bi bi-receipt text-danger"></i><span>Expenses</span>
        </a>
'''

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<!-- Expenses -->' in content or '@{/expenses}' in content:
        # Check if it's already conditional
        if 'enable_expenses' not in content:
            new_content = re.sub(pattern, new_text, content)
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1

print(f'Made Expenses conditional in {count} files.')
