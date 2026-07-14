import glob

issues = []
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    has_sidebar = 'sidebar-nav' in content
    has_classappend = 'th:classappend' in content
    has_requesturi = 'requestURI' in content
    has_old_httpservlet = '#httpServletRequest' in content

    if has_sidebar:
        issues.append({
            'file': filepath.replace('src/main/resources/templates/', ''),
            'has_classappend': has_classappend,
            'has_requesturi': has_requesturi,
            'has_old': has_old_httpservlet,
        })

print(f'Total templates WITH sidebar: {len(issues)}')
print()
print('--- MISSING classappend or requestURI ---')
missing = [i for i in issues if not i['has_classappend'] or not i['has_requesturi']]
for i in missing:
    print(f"  {i['file']}  classappend={i['has_classappend']}  requestURI={i['has_requesturi']}")

print()
print('--- STILL using old #httpServletRequest ---')
old = [i for i in issues if i['has_old']]
for i in old:
    print(f"  {i['file']}")
