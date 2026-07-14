"""
Fix sidebar scroll: inject inline scroll script into every template that has a sidebar.
This replaces the unreliable external app.js approach.
"""
import glob
import re

SCROLL_SCRIPT = """
<script>
// Sidebar scroll - runs inline, no timing issues
(function() {
    function scrollSidebarToActive() {
        var nav = document.querySelector('.sidebar-nav');
        var active = document.querySelector('.sidebar-nav .nav-link.active');
        if (!nav || !active) return;
        var navH = nav.clientHeight;
        var top  = active.offsetTop;
        var h    = active.offsetHeight;
        nav.scrollTop = Math.max(0, top - navH / 2 + h / 2);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scrollSidebarToActive);
    } else {
        scrollSidebarToActive();
    }
})();
</script>"""

# Pattern: the closing </body> tag
BODY_CLOSE = re.compile(r'</body>', re.IGNORECASE)

updated = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'sidebar-nav' not in content:
        continue

    # Remove old inline scroll scripts we may have added
    content = re.sub(r'\n<script>\n// Sidebar scroll.*?</script>', '', content, flags=re.DOTALL)

    # Insert before </body>
    if BODY_CLOSE.search(content):
        new_content = BODY_CLOSE.sub(SCROLL_SCRIPT + '\n</body>', content, count=1)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        updated += 1

print(f"Updated {updated} templates with inline sidebar scroll script.")
