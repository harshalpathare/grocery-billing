import os, glob, re

new_text = '''
        <!-- Accounting -->
        <a th:href="@{/accounting/dashboard}" class="nav-link" th:classappend="${activePage == 'accounting' ? 'active' : ''}">
            <i class="bi bi-book-half text-secondary"></i><span>Accounting</span>
        </a>
'''

# The old text was:
#        <!-- Accounting -->
#        <div class="nav-item">
#            <a href="#accountingSubmenu" data-bs-toggle="collapse" class="nav-link collapsed">
#                <i class="bi bi-book-half text-secondary"></i>
#                <span>Accounting</span>
#                <i class="bi bi-chevron-down ms-auto" style="font-size: 0.8rem;"></i>
#            </a>
#            <div class="collapse" id="accountingSubmenu">
#                <ul class="nav flex-column ms-3">
#                    <li class="nav-item"><a th:href="@{/accounting/journal}" class="nav-link"><i class="bi bi-journal-text me-2"></i>Journal</a></li>
#                    <li class="nav-item"><a th:href="@{/accounting/ledger}" class="nav-link"><i class="bi bi-journal-album me-2"></i>Ledger</a></li>
#                    <li class="nav-item"><a th:href="@{/accounting/cashbook}" class="nav-link"><i class="bi bi-wallet2 me-2"></i>Cash Book</a></li>
#                    <li class="nav-item"><a th:href="@{/accounting/bankbook}" class="nav-link"><i class="bi bi-bank me-2"></i>Bank Book</a></li>
#                    <li class="nav-item"><a th:href="@{/accounting/trialbalance}" class="nav-link"><i class="bi bi-bar-chart-steps me-2"></i>Trial Balance</a></li>
#                </ul>
#            </div>
#        </div>

pattern = re.compile(r'[ \t]*<!-- Accounting -->.*?</div>\s*</div>', re.DOTALL)

count = 0
for filepath in glob.glob('src/main/resources/templates/**/*.html', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'accountingSubmenu' in content:
        new_content = re.sub(pattern, new_text, content)
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1

print(f'Replaced Accounting submenu with Dashboard link in {count} files.')
