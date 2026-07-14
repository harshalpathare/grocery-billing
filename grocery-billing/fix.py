import os
import glob

files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)

search1 = '<a th:href="@{/credit}" class="nav-link">'
search2 = '<a th:href="@{/credit}" class="nav-link" th:if="`${features[\'enable_credit\'] == true}`">'
search3 = '<a th:href="@{/credit}" class="nav-link" th:if="`${features[\'\'enable_credit\'\'] == true}`">'
replacement = '<a th:href="@{/credit}" class="nav-link" th:if="${features[\'enable_credit\'] == true}">'

for f in files:
    try:
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            
        new_content = content.replace(search1, replacement).replace(search2, replacement).replace(search3, replacement)
        
        if content != new_content:
            with open(f, 'w', encoding='utf-8') as file:
                file.write(new_content)
            print("Fixed:", f)
    except Exception as e:
        print("Error on", f, e)
