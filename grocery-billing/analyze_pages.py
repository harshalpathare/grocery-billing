import glob

def analyze(path):
    with open(path, encoding='utf-8') as f:
        content = f.read()
    lines = content.split('\n')
    keywords = ['<head', '</head', '<body', 'sidebar', 'mainContent', 'sidebarToggle', 'app.js', 'style.css', '</body']
    print(f"\n=== {path} ===")
    for i, line in enumerate(lines):
        stripped = line.strip()
        if any(k in stripped for k in keywords):
            print(f"  L{i+1}: {stripped[:120]}")

# Analyze expenses page and one working page for comparison
analyze('src/main/resources/templates/expenses/list.html')
analyze('src/main/resources/templates/dashboard/index.html')
