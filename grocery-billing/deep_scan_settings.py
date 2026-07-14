"""
Deep scan: find any Settings nav-link that is NOT enclosed by a sidebar-bottom div.
Uses proper DOM-structure analysis.
"""
import glob, re

for fp in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    with open(fp, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'sidebar-nav' not in content or '/settings' not in content:
        continue

    # Find all <a> links to /settings with class="nav-link"
    settings_links = list(re.finditer(
        r'<a\s[^>]*?th:href="@\{/settings\}"[^>]*?class="nav-link"[^>]*?>|<a\s[^>]*?class="nav-link"[^>]*?th:href="@\{/settings\}"[^>]*?>',
        content, re.DOTALL
    ))

    if not settings_links:
        continue

    name = fp.replace('src\\main\\resources\\templates\\', '').replace('src/main/resources/templates/', '')

    for match in settings_links:
        pos = match.start()
        # What is the nearest enclosing div with class?
        # Look backwards for the nearest <div class="sidebar-bottom"> or <nav class="sidebar-nav">
        before = content[:pos]
        last_sidebar_bottom = before.rfind('sidebar-bottom')
        last_sidebar_nav_open = before.rfind('<nav class="sidebar-nav">')
        last_sidebar_nav_close = before.rfind('</nav>')

        # Is this Settings link inside sidebar-nav? (nav opened, not yet closed)
        in_nav = last_sidebar_nav_open > last_sidebar_nav_close if last_sidebar_nav_open != -1 else False
        in_bottom = last_sidebar_bottom > last_sidebar_nav_close

        location = 'IN-NAV' if in_nav else ('IN-BOTTOM' if in_bottom else 'UNKNOWN')
        print(name + ': Settings at pos=' + str(pos) + ' -> ' + location)
