import os
import re

directory = 'src/main/resources/templates'

replacements = {
    r'bi-speedometer2': 'bi-grid-1x2-fill',
    r'bi-box-seam': 'bi-box-fill',
    r'bi-truck': 'bi-truck-front-fill',
    r'bi-cart-check': 'bi-bag-check-fill',
    r'bi-people': 'bi-people-fill',
    r'bi-receipt': 'bi-file-earmark-plus-fill',
    r'bi-journal-text': 'bi-journals',
    r'bi-credit-card': 'bi-credit-card-fill',
    r'bi-bar-chart': 'bi-pie-chart-fill',
    r'bi-gear': 'bi-gear-fill'
}

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.html'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original = content
            for old, new in replacements.items():
                content = re.sub(r'\b' + old + r'\b', new, content)
                
            if content != original:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Updated {filepath}')
