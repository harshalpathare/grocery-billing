import glob, re

results_old_css = []
results_old_js = []
results_ok = []

for fp in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()

    if 'sidebar-nav' not in c:
        continue

    css_match = re.search(r'style\.css\?v=(\d+)', c)
    js_match  = re.search(r'app\.js\?v=(\d+)', c)

    css_v = int(css_match.group(1)) if css_match else 0
    js_v  = int(js_match.group(1))  if js_match  else 0

    name = fp.replace('src\\main\\resources\\templates\\', '').replace('src/main/resources/templates/', '')

    if css_v < 5:
        results_old_css.append(name + ' (css=v' + str(css_v) + ')')
    if js_v < 20:
        results_old_js.append(name + ' (js=v' + str(js_v) + ')')
    if css_v >= 5 and js_v >= 20:
        results_ok.append(name)

print('=== OLD CSS (needs v5) ===')
for x in results_old_css: print(' ', x)
print()
print('=== OLD JS (needs v20) ===')
for x in results_old_js: print(' ', x)
print()
print('OK: ' + str(len(results_ok)) + ' templates up to date')
print('Total checked: ' + str(len(results_old_css) + len(results_old_js) + len(results_ok)))
