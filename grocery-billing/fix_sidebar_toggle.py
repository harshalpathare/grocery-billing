import glob
import re

files_updated = []

for f in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()

    original = content

    # 1. Add defer to app.js script tag (makes it execute after DOM is fully parsed, no DOMContentLoaded needed)
    content = re.sub(
        r'<script (th:src="@\{/js/app\.js\?v=\d+\}")>',
        r'<script defer \1>',
        content
    )

    # 2. Add type="button" to sidebarToggle button to prevent any form submit behavior
    content = re.sub(
        r'<button([^>]*?)id="sidebarToggle"([^>]*?)>',
        lambda m: f'<button{m.group(1)}id="sidebarToggle"{m.group(2)} type="button">',
        content
    )
    # Handle already-has-type-button case (avoid duplicates)
    content = re.sub(r' type="button"([^>]*?) type="button"', r' type="button"\1', content)

    if content != original:
        with open(f, 'w', encoding='utf-8') as file:
            file.write(content)
        files_updated.append(f)
        print(f'Updated: {f}')

print(f'\nTotal files updated: {len(files_updated)}')
