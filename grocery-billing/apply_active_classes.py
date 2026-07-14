import os
import glob
import re

print("Applying th:classappend to all sidebars for server-side active state...")

link_patterns = {
    '@{/}': 'th:classappend="${#httpServletRequest.requestURI == \'/\'} ? \'active\' : \'\'"',
    '@{/super/shops}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/super/\')} ? \'active\' : \'\'"',
    '@{/products}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/products\')} ? \'active\' : \'\'"',
    '@{/suppliers}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/suppliers\')} ? \'active\' : \'\'"',
    '@{/purchases}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/purchases\')} ? \'active\' : \'\'"',
    '@{/customers}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/customers\')} ? \'active\' : \'\'"',
    '@{/bills/new}': 'th:classappend="${#httpServletRequest.requestURI != null and (#httpServletRequest.requestURI == \'/bills/new\' or #httpServletRequest.requestURI == \'/bills/quick\')} ? \'active\' : \'\'"',
    '@{/bills}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/bills\') and #httpServletRequest.requestURI != \'/bills/new\' and !#httpServletRequest.requestURI.contains(\'/quick\')} ? \'active\' : \'\'"',
    '@{/credit}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/credit\')} ? \'active\' : \'\'"',
    '@{/cashflow}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/cashflow\')} ? \'active\' : \'\'"',
    '@{/inventory}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/inventory\')} ? \'active\' : \'\'"',
    '@{/expenses}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/expenses\')} ? \'active\' : \'\'"',
    '@{/accounting/dashboard}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/accounting\')} ? \'active\' : \'\'"',
    '@{/reports}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/reports\')} ? \'active\' : \'\'"',
    '@{/settings}': 'th:classappend="${#httpServletRequest.requestURI != null and #httpServletRequest.requestURI.startsWith(\'/settings\')} ? \'active\' : \'\'"'
}

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content
    
    for href, classappend in link_patterns.items():
        pattern = r'(<a\s+[^>]*th:href="' + re.escape(href) + r'"[^>]*class="nav-link"[^>]*)>'
        
        def replacer(match):
            tag_content = match.group(1)
            tag_content = re.sub(r'\s*th:classappend="[^"]*"', '', tag_content)
            return tag_content + ' ' + classappend + '>'
            
        new_content = re.sub(pattern, replacer, new_content)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += 1

print(f"Updated {count} files.")
