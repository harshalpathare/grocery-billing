import os
import re

# We will read `base.html` to get the header and footer
with open('src/main/resources/templates/layout/base.html', 'r', encoding='utf-8') as f:
    base_html = f.read()

# We need to split `base.html` into header and footer.
# Let's split right after `<div class="page-content">` and before `</div>\n    </div>\n</div>\n\n<!-- Scripts -->`
# Wait, let's just split on `<!-- Greeting -->`
header_split = base_html.split('<!-- Greeting -->')
header_part = header_split[0]

# Now for the footer, let's split on `</div>\n\n    </div>\n</div>\n\n<script src="https://cdn.jsdelivr.net/`
# Actually, we can just split on `</div>\n\n    </div>\n</div>\n\n<script src="`
import re
footer_match = re.search(r'</div>\s*</div>\s*</div>\s*<script src="https://cdn.jsdelivr.net/', base_html)
if footer_match:
    footer_part = base_html[footer_match.start():]
else:
    # fallback
    footer_match = re.search(r'</div>\s*</div>\s*</div>\s*<script ', base_html)
    footer_part = base_html[footer_match.start():]

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract just the <div class="container-fluid py-4">...</div> part
    content_match = re.search(r'<div class="container-fluid py-4">(.*)</div>\s*</body>', content, re.DOTALL)
    
    if content_match:
        inner_content = content_match.group(0)
        # Construct the new HTML
        new_html = header_part + inner_content + '\n' + footer_part
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_html)
        print("Updated", filepath)

process_file('src/main/resources/templates/accounting/dashboard.html')
process_file('src/main/resources/templates/accounting/journal.html')
process_file('src/main/resources/templates/accounting/ledger.html')
process_file('src/main/resources/templates/accounting/book_view.html')
process_file('src/main/resources/templates/accounting/trial_balance.html')
