import glob, re

script_pattern = re.compile(r'(\s*<script>\s*\(function\(\)\{\s*var nav = document\.querySelector\(\'\.sidebar-nav\'\);\s*var active = nav \? nav\.querySelector\(\'\.nav-link\.active\'\) : null;\s*if \(nav && active\) \{\s*var target = active\.offsetTop - Math\.floor\(nav\.clientHeight / 3\);\s*nav\.scrollTop = Math\.max\(0, Math\.min\(target, nav\.scrollHeight - nav\.clientHeight\)\);\s*\}\s*\}\)\(\);\s*</script>\s*)')

updated = 0

for fp in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    
    # Check if the script is in this file
    match = script_pattern.search(c)
    if not match:
        continue
    
    script_str = match.group(1)
    
    # Remove the script from its current location
    c_without_script = c[:match.start()] + c[match.end():]
    
    # Find the closing tag of sidebar just before main-content
    # We look for "</div>\s*<div class="main-content""
    injection_pattern = re.compile(r'(</div>\s*<div class="main-content")')
    
    inject_match = injection_pattern.search(c_without_script)
    if inject_match:
        # Re-inject the script right before the closing </div> of sidebar
        # wait, the regex captures "</div> \n <div class="main-content""
        # we want to inject it before that whole capture. But the script belongs inside the sidebar?
        # Actually, if we put the script right BEFORE `</div>`, it's inside the sidebar.
        # But wait, it doesn't matter if it's inside or outside the sidebar div, as long as it executes AFTER sidebar-bottom!
        # If we put it right BEFORE `<div class="main-content"`, it will execute exactly when sidebar is fully closed!
        nc = c_without_script[:inject_match.start()] + script_str + c_without_script[inject_match.start():]
        
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(nc)
        updated += 1
    else:
        print("Couldn't find insertion point in", fp)

print("Moved inline script in", updated, "files.")
