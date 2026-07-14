import glob

script_to_inject = """
    <!-- Scrollbar Fix -->
    <script th:inline="javascript">
    /*<![CDATA[*/
    (function(){
        var nav = document.querySelector('.sidebar-nav');
        if(nav) {
            var active = nav.querySelector('.nav-link.active');
            if(active) {
                var diff = (active.offsetTop + active.offsetHeight) - nav.clientHeight;
                if(diff > 0) {
                    nav.scrollTop = diff + 20;
                }
            }
        }
    })();
    /*]]>*/
    </script>
    </nav>
"""

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<nav class="sidebar-nav">' in content and 'Scrollbar Fix' not in content:
        # Replace the first closing </nav> with our script and closing </nav>
        new_content = content.replace('</nav>', script_to_inject, 1)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += 1

print(f"Injected smooth scroll script into {count} files")
