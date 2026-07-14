import os
import glob
import re

def fix_sidebar(filepath, active_link_hrefs):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Remove 'active' from all nav-links
    # Note: we need to match class="nav-link active" or class="nav-link fw-semibold active" etc.
    # Be careful not to remove other classes.
    # Actually, simpler: replace 'class="nav-link active"' with 'class="nav-link"'
    # and remove th:classappend="...active..." if any
    
    content = content.replace('class="nav-link active"', 'class="nav-link"')
    content = content.replace('class="nav-link fw-semibold active"', 'class="nav-link fw-semibold"')
    content = re.sub(r'th:classappend="[^"]*active[^"]*"', '', content)
    
    # 2. Add 'active' to the correct link(s)
    # The links look like: <a th:href="@{/cashflow}" class="nav-link"...
    for href in active_link_hrefs:
        # regex to find <a th:href="@{href}" class="nav-link"
        pattern = r'(<a\s+[^>]*th:href="@\{' + re.escape(href) + r'\}"[^>]*class="nav-link)([^"]*)(")'
        content = re.sub(pattern, r'\1 active\2\3', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# Define rules
# folder -> list of hrefs that should be active
rules = {
    'cashflow': ['/cashflow'],
    'accounting': ['/accounting/dashboard'],
    'expenses': ['/expenses'],
    'inventory': ['/inventory'],
    'bill': ['/bills'],
    'customer': ['/customers'],
    'dashboard': ['/'],
    'product': ['/products'],
    'purchase': ['/purchases'],
    'report': ['/reports'],
    'settings': ['/settings'],
    'super': ['/super/shops'],
    'supplier': ['/suppliers'],
    'credit': ['/credit']
}

base_dir = 'src/main/resources/templates'

for folder, hrefs in rules.items():
    pattern = os.path.join(base_dir, folder, '**/*.html')
    for filepath in glob.glob(pattern, recursive=True):
        fix_sidebar(filepath, hrefs)

print("Sidebars fixed successfully.")
