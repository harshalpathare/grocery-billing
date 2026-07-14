import glob, re

INLINE_SCRIPT = """
        <script>
            (function(){
                var nav = document.querySelector('.sidebar-nav');
                var active = nav ? nav.querySelector('.nav-link.active') : null;
                if (nav && active) {
                    var target = active.offsetTop - Math.floor(nav.clientHeight / 3);
                    nav.scrollTop = Math.max(0, Math.min(target, nav.scrollHeight - nav.clientHeight));
                }
            })();
        </script>
"""

updated = 0
for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<nav class="sidebar-nav">' not in content:
        continue
    
    # Check if already injected
    if 'var target = active.offsetTop - Math.floor(nav.clientHeight / 3);' in content:
        continue
        
    # Find </nav> corresponding to the sidebar
    # We replace the LAST </nav> before sidebar-bottom or just replace the first </nav> after sidebar-nav
    # Since there is only one <nav class="sidebar-nav">, we can do a regex replacement.
    
    new_content = re.sub(
        r'(</nav>)\s*(<div class="sidebar-bottom">)', 
        r'\1' + INLINE_SCRIPT + r'    \2', 
        content
    )
    
    if new_content != content:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(new_content)
        updated += 1
        print("Updated:", fp)

print("Total updated:", updated)
