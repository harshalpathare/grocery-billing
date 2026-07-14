import glob, re

script_pattern = re.compile(r'(\s*<script>\s*\(function\(\)\{\s*var nav = document\.querySelector\(\'\.sidebar-nav\'\);\s*var active = nav \? nav\.querySelector\(\'\.nav-link\.active\'\) : null;\s*if \(nav && active\) \{\s*var target = active\.offsetTop - Math\.floor\(nav\.clientHeight / 3\);\s*nav\.scrollTop = Math\.max\(0, Math\.min\(target, nav\.scrollHeight - nav\.clientHeight\)\);\s*\}\s*\}\)\(\);\s*</script>\s*)')

updated = 0
for fp in ['src/main/resources/templates/dashboard/index.html', 'src/main/resources/templates/product/form.html', 'src/main/resources/templates/product/list.html']:
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    
    match = script_pattern.search(c)
    if not match:
        print('Script not found in', fp)
        continue
    
    script_str = match.group(1)
    c_without_script = c[:match.start()] + c[match.end():]
    
    # looser injection point
    injection_pattern = re.compile(r'(<div class="main-content")')
    inject_match = injection_pattern.search(c_without_script)
    if inject_match:
        nc = c_without_script[:inject_match.start()] + script_str + c_without_script[inject_match.start():]
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(nc)
        updated += 1
    else:
        print('Failed to inject', fp)

print('Updated', updated, 'files')
