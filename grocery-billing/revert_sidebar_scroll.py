import glob

script_to_remove = """
    </nav>
    <script>
        (function(){
            var nav = document.querySelector('.sidebar-nav');
            var active = document.querySelector('.sidebar-nav .nav-link.active');
            if(nav && active) {
                var pos = sessionStorage.getItem('sidebarScrollPos');
                if(pos !== null) {
                    nav.scrollTop = parseInt(pos, 10);
                } else {
                    var linkBottom = active.offsetTop + active.offsetHeight;
                    if(linkBottom > nav.clientHeight) {
                        nav.scrollTop = linkBottom - nav.clientHeight + 20;
                    }
                }
            }
            if(nav) {
                nav.addEventListener('click', function() {
                    sessionStorage.setItem('sidebarScrollPos', nav.scrollTop);
                });
            }
        })();
    </script>
"""

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if script_to_remove in content:
        new_content = content.replace(script_to_remove, '</nav>')
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += 1

print(f"Reverted script in {count} files")
