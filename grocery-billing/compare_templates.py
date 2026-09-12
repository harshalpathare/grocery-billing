import re

def parse_html(path):
    with open(path, 'r', encoding='utf-8') as f:
        html = f.read()
    
    # Extract sidebar
    sidebar = re.search(r'<div[^>]*id="sidebar"[^>]*>.*?(?=<!-- \={21} MAIN CONTENT \={21} -->|<div class="main-content")', html, re.DOTALL)
    
    # Extract main content start
    main = re.search(r'<div class="main-content" id="mainContent">.*?<nav class="top-navbar">.*?</nav>', html, re.DOTALL)
    
    # Extract scripts at the bottom
    scripts = re.findall(r'<script.*?</script>', html, re.DOTALL)
    
    return {
        'sidebar': sidebar.group(0) if sidebar else None,
        'main': main.group(0) if main else None,
        'scripts': scripts
    }

exp = parse_html('src/main/resources/templates/expenses/list.html')
prod = parse_html('src/main/resources/templates/product/list.html')

print("=== SCRIPT DIFFERENCES ===")
print("Expenses scripts:")
for s in exp['scripts']:
    print("  ", s.split('\n')[0][:100])

print("\nProducts scripts:")
for s in prod['scripts']:
    print("  ", s.split('\n')[0][:100])

print("\n=== TOGGLE BTN IN MAIN ===")
print("Expenses:", 'sidebarToggle' in exp['main'] if exp['main'] else False)
print("Products:", 'sidebarToggle' in prod['main'] if prod['main'] else False)
