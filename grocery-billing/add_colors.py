import os
import re

directory = 'src/main/resources/templates'

replacements = {
    r'class="bi bi-grid-1x2-fill"': 'class="bi bi-grid-1x2-fill text-primary"',
    r'class="bi bi-box-fill"': 'class="bi bi-box-fill text-success"',
    r'class="bi bi-truck-front-fill"': 'class="bi bi-truck-front-fill text-warning"',
    r'class="bi bi-bag-check-fill"': 'class="bi bi-bag-check-fill text-info"',
    r'class="bi bi-people-fill"': 'class="bi bi-people-fill text-danger"',
    r'class="bi bi-file-earmark-plus-fill"': 'class="bi bi-file-earmark-plus-fill text-primary"',
    r'class="bi bi-journals"': 'class="bi bi-journals text-secondary"',
    r'class="bi bi-credit-card-fill"': 'class="bi bi-credit-card-fill text-danger"',
    r'class="bi bi-pie-chart-fill"': 'class="bi bi-pie-chart-fill text-success"',
    r'class="bi bi-gear-fill"': 'class="bi bi-gear-fill text-secondary"'
}

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.html'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original = content
            for old, new in replacements.items():
                content = content.replace(old, new)
                
            if content != original:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Updated {filepath}')
