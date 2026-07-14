import glob

script_to_remove = """
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
"""

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<!-- Scrollbar Fix -->' in content:
        new_content = content.replace(script_to_remove, '')
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += 1

print(f"Reverted smooth scroll script in {count} files")
