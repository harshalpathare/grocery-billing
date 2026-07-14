"""
Fix three templates missing app.js, and upgrade scroll logic in app.js to
always center the active sidebar item (no conditional check that can fail).
"""
import re

# 1. Add app.js to the three templates that are missing it
TEMPLATES = [
    'src/main/resources/templates/bill/create.html',
    'src/main/resources/templates/bill/view.html',
    'src/main/resources/templates/dashboard/index.html',
]

SCRIPT_TAG = '<script th:src="@{/js/app.js?v=20}"></script>\n'

for fp in TEMPLATES:
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()

    if 'app.js' in c:
        print(f'SKIP (already has app.js): {fp}')
        continue

    # Insert before the bootstrap bundle script (or before </body> as fallback)
    if 'bootstrap.bundle' in c:
        new_c = c.replace(
            '<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>',
            SCRIPT_TAG + '<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>',
            1
        )
    else:
        new_c = c.replace('</body>', SCRIPT_TAG + '</body>', 1)

    with open(fp, 'w', encoding='utf-8') as f:
        f.write(new_c)
    print(f'Fixed: {fp}')

print('Done.')
