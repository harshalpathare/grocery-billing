import re
from collections import Counter

content = open('src/main/resources/templates/expenses/list.html', encoding='utf-8').read()
ids = re.findall(r'id="([^"]+)"', content)
print('All IDs in expenses/list.html:')
counted = Counter(ids)
for id_val, count in sorted(counted.items()):
    flag = ' <-- DUPLICATE!' if count > 1 else ''
    print(f'  {id_val}: {count}{flag}')
