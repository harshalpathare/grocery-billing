"""
Find and fix duplicate Settings links in sidebar templates.
Design decision: Settings belongs in .sidebar-bottom (pinned at bottom).
Remove any Settings <a> link that appears INSIDE .sidebar-nav.
"""
import glob, re

# Pattern: a link to /settings inside sidebar-nav (not sidebar-bottom)
# We'll find the sidebar-nav block in each template and remove Settings from it.

SETTINGS_LINK_RE = re.compile(
    r'\s*<a\s[^>]*th:href="@\{/settings\}"[^>]*>.*?</a>\s*',
    re.DOTALL | re.IGNORECASE
)

updated = []
skipped = []

for fp in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    with open(fp, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'sidebar-nav' not in content:
        continue

    # Find sidebar-nav block
    nav_start = content.find('<nav class="sidebar-nav">')
    nav_end   = content.find('</nav>', nav_start)
    if nav_start == -1 or nav_end == -1:
        continue

    nav_block = content[nav_start:nav_end + 6]

    if '/settings' not in nav_block:
        skipped.append(fp)
        continue

    # Remove Settings link from inside the nav block
    new_nav_block = SETTINGS_LINK_RE.sub('\n        ', nav_block)
    # Clean up multiple blank lines
    new_nav_block = re.sub(r'\n\s*\n\s*\n', '\n\n', new_nav_block)

    if new_nav_block == nav_block:
        skipped.append(fp)
        continue

    new_content = content[:nav_start] + new_nav_block + content[nav_end + 6:]

    with open(fp, 'w', encoding='utf-8') as f:
        f.write(new_content)

    name = fp.replace('src\\main\\resources\\templates\\', '').replace('src/main/resources/templates/', '')
    updated.append(name)
    print('FIXED: ' + name)

print()
print('Updated ' + str(len(updated)) + ' templates.')
print('Skipped (no dup): ' + str(len(skipped)))
