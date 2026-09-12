import glob, re

files = glob.glob('src/main/resources/templates/**/*.html', recursive=True)

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    
    # 1. Inject Hamburger
    if '<nav class="top-navbar">' in content and 'sidebarToggle' not in content:
        if '<div class="sidebar" id="sidebar">' in content or 'th:replace="layout/base :: sidebar"' in content:
            # We want to insert the button right after <nav class="top-navbar">
            pattern = r'(<nav class="top-navbar">)'
            replacement = r'\1\n        <button class="btn btn-sm btn-outline-secondary" id="sidebarToggle"><i class="bi bi-list"></i></button>'
            content = re.sub(pattern, replacement, content, count=1)
            
    # 2. Inject table-responsive
    if '<table' in content and 'table-responsive' not in content:
        # Wrap tables with <div class="table-responsive"> if not already wrapped
        # Actually this is hard to do with regex perfectly, but I'll do a simple find/replace for specific patterns.
        # It's better to manually replace the few that I found earlier: bill/quick.html
        pass

    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
            
print("Hamburger buttons injected.")
